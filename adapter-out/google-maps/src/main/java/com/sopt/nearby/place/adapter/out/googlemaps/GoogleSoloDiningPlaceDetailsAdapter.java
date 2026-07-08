// Google Places Details API로 혼밥 장소 상세 정보를 조회하는 어댑터다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleSoloDiningPlaceDetailsAdapter implements SoloDiningPlaceDetailsPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleSoloDiningPlaceDetailsAdapter.class);
    private static final URI GOOGLE_PLACES_BASE_URI = URI.create("https://places.googleapis.com/v1/");
    private static final String FIELD_MASK = "id,displayName,formattedAddress,location,primaryType,types,rating,"
            + "userRatingCount,nationalPhoneNumber,photos,businessStatus,priceLevel,priceRange,"
            + "regularOpeningHours,editorialSummary";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI baseUri;
    private final Duration timeout;

    @Autowired
    public GoogleSoloDiningPlaceDetailsAdapter(
            @Value("${nearby.google.places.api-key:}") final String apiKey,
            @Value("${nearby.google.places.timeout-ms:1000}") final long timeoutMs
    ) {
        this(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                apiKey,
                GOOGLE_PLACES_BASE_URI,
                Duration.ofMillis(timeoutMs)
        );
    }

    GoogleSoloDiningPlaceDetailsAdapter(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final String apiKey,
            final URI baseUri,
            final Duration timeout
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUri = baseUri;
        this.timeout = timeout;
    }

    @Override
    public SoloDiningPlaceDetailsResult findByGooglePlaceId(final String googlePlaceId) {
        if (isBlank(apiKey) || isBlank(googlePlaceId)) {
            throw new GooglePlaceApiException();
        }

        HttpRequest request = HttpRequest.newBuilder(detailsUri(googlePlaceId))
                .timeout(timeout)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .GET()
                .build();

        try {
            return toResult(send(request));
        } catch (GooglePlaceApiException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.debug("Google Places details mapping failed.", exception);
            throw new GooglePlaceApiException();
        }
    }

    private URI detailsUri(final String googlePlaceId) {
        return baseUri.resolve("places/" + pathSegment(googlePlaceId) + "?languageCode=ko&regionCode=KR");
    }

    private GooglePlace send(final HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new GooglePlaceApiException();
            }
            return objectMapper.readValue(response.body(), GooglePlace.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GooglePlaceApiException();
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Google Places details failed.", exception);
            throw new GooglePlaceApiException();
        }
    }

    private SoloDiningPlaceDetailsResult toResult(final GooglePlace place) {
        return new SoloDiningPlaceDetailsResult(
                place.id(),
                place.displayName() == null ? null : place.displayName().text(),
                place.formattedAddress(),
                place.location() == null ? null : place.location().latitude(),
                place.location() == null ? null : place.location().longitude(),
                category(place),
                place.rating(),
                place.userRatingCount(),
                place.nationalPhoneNumber(),
                firstPhotoName(place.photos()),
                photoNames(place.photos()),
                businessStatus(place.businessStatus()),
                priceLevel(place.priceLevel()),
                priceRange(place.priceRange()),
                place.regularOpeningHours() == null ? List.of() : place.regularOpeningHours().weekdayDescriptions(),
                place.editorialSummary() == null ? null : place.editorialSummary().text()
        );
    }

    private SoloDiningPlaceCategory category(final GooglePlace place) {
        List<String> types = new ArrayList<>();
        if (!isBlank(place.primaryType())) {
            types.add(place.primaryType());
        }
        if (place.types() != null) {
            types.addAll(place.types());
        }
        if (types.stream().anyMatch(type -> type != null
                && (type.equals("cafe") || type.equals("coffee_shop") || type.endsWith("_cafe")))) {
            return SoloDiningPlaceCategory.CAFE;
        }
        if (types.stream().anyMatch(type -> type != null && (type.equals("pub") || type.equals("bar")))) {
            return SoloDiningPlaceCategory.PUB;
        }
        if (types.stream().anyMatch(type -> type != null && (type.equals("restaurant")
                || type.endsWith("_restaurant")))) {
            return SoloDiningPlaceCategory.RESTAURANT;
        }
        return null;
    }

    private PlaceBusinessStatus businessStatus(final String value) {
        try {
            return value == null ? PlaceBusinessStatus.UNKNOWN : PlaceBusinessStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return PlaceBusinessStatus.UNKNOWN;
        }
    }

    private String priceLevel(final String value) {
        return value == null || "PRICE_LEVEL_UNSPECIFIED".equals(value) ? null : value;
    }

    private String priceRange(final PriceRange priceRange) {
        if (priceRange == null) {
            return null;
        }
        String start = money(priceRange.startPrice());
        if (start == null) {
            return null;
        }
        String end = money(priceRange.endPrice());
        return end == null ? start + " 이상" : start + "~" + end;
    }

    private String money(final Money money) {
        if (money == null || isBlank(money.units())) {
            return null;
        }
        String symbol = "KRW".equals(money.currencyCode()) ? "₩" : money.currencyCode() + " ";
        return symbol + NumberFormat.getIntegerInstance(Locale.KOREA).format(new BigDecimal(money.units()));
    }

    private String firstPhotoName(final List<GooglePhoto> photos) {
        return photos == null || photos.isEmpty() ? null : photos.getFirst().name();
    }

    private List<String> photoNames(final List<GooglePhoto> photos) {
        if (photos == null) {
            return List.of();
        }
        return photos.stream()
                .map(GooglePhoto::name)
                .filter(value -> !isBlank(value))
                .toList();
    }

    private String pathSegment(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlace(
            String id,
            LocalizedText displayName,
            String formattedAddress,
            Location location,
            String primaryType,
            List<String> types,
            BigDecimal rating,
            Integer userRatingCount,
            String nationalPhoneNumber,
            List<GooglePhoto> photos,
            String businessStatus,
            String priceLevel,
            PriceRange priceRange,
            OpeningHours regularOpeningHours,
            LocalizedText editorialSummary
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LocalizedText(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Location(BigDecimal latitude, BigDecimal longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePhoto(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PriceRange(Money startPrice, Money endPrice) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Money(String currencyCode, String units) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpeningHours(List<String> weekdayDescriptions) {
        private OpeningHours {
            weekdayDescriptions = weekdayDescriptions == null ? List.of() : weekdayDescriptions;
        }
    }
}
