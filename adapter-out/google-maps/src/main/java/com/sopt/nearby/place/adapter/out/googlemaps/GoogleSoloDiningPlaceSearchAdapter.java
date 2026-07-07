// Google Places Nearby Search API로 주변 혼밥 장소를 조회하는 어댑터다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleSoloDiningPlaceSearchAdapter implements SoloDiningPlaceSearchPort {

    private static final Logger log = LoggerFactory.getLogger(GoogleSoloDiningPlaceSearchAdapter.class);
    private static final URI GOOGLE_PLACES_BASE_URI = URI.create("https://places.googleapis.com/v1/");
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,"
            + "places.location,places.rating,places.userRatingCount,places.photos,places.businessStatus,"
            + "places.primaryType,places.types";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI baseUri;
    private final Duration timeout;

    @Autowired
    public GoogleSoloDiningPlaceSearchAdapter(
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

    GoogleSoloDiningPlaceSearchAdapter(
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
    public List<SoloDiningPlaceSearchResult> search(final SoloDiningPlaceSearchRequest request) {
        if (isBlank(apiKey)) {
            throw new GooglePlaceApiException();
        }

        HttpRequest httpRequest = HttpRequest.newBuilder(nearbySearchUri())
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", FIELD_MASK)
                .POST(HttpRequest.BodyPublishers.ofString(body(request)))
                .build();

        return send(httpRequest)
                .places()
                .stream()
                .map(this::toResult)
                .toList();
    }

    private URI nearbySearchUri() {
        String value = baseUri.toString();
        return URI.create((value.endsWith("/") ? value : value + "/") + "places:searchNearby");
    }

    private String body(final SoloDiningPlaceSearchRequest request) {
        try {
            return objectMapper.writeValueAsString(new NearbySearchRequest(
                    request.includedTypes(),
                    request.maxResultCount(),
                    new LocationRestriction(new Circle(
                            new Center(request.latitude(), request.longitude()),
                            request.radiusMeters()
                    )),
                    "DISTANCE"
            ));
        } catch (IOException exception) {
            throw new GooglePlaceApiException();
        }
    }

    private NearbySearchResponse send(final HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new GooglePlaceApiException();
            }
            return objectMapper.readValue(response.body(), NearbySearchResponse.class);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new GooglePlaceApiException();
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Google Places nearby search failed.", exception);
            throw new GooglePlaceApiException();
        }
    }

    private SoloDiningPlaceSearchResult toResult(final GooglePlace place) {
        return new SoloDiningPlaceSearchResult(
                place.id(),
                place.displayName() == null ? null : place.displayName().text(),
                place.formattedAddress(),
                place.location() == null ? null : place.location().latitude(),
                place.location() == null ? null : place.location().longitude(),
                category(place),
                place.rating(),
                place.userRatingCount(),
                firstPhotoName(place.photos()),
                businessStatus(place.businessStatus())
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
        if (types.stream().anyMatch(type -> type != null && (type.equals("cafe") || type.endsWith("_cafe")))) {
            return SoloDiningPlaceCategory.CAFE;
        }
        if (types.stream().anyMatch("pub"::equals)) {
            return SoloDiningPlaceCategory.PUB;
        }
        if (types.stream().anyMatch(type -> type != null && (type.equals("restaurant")
                || type.endsWith("_restaurant")))) {
            return SoloDiningPlaceCategory.RESTAURANT;
        }
        return SoloDiningPlaceCategory.OTHER;
    }

    private PlaceBusinessStatus businessStatus(final String value) {
        try {
            return value == null ? PlaceBusinessStatus.UNKNOWN : PlaceBusinessStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return PlaceBusinessStatus.UNKNOWN;
        }
    }

    private String firstPhotoName(final List<GooglePhoto> photos) {
        return photos == null || photos.isEmpty() ? null : photos.getFirst().name();
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private record NearbySearchRequest(
            List<String> includedTypes,
            int maxResultCount,
            LocationRestriction locationRestriction,
            String rankPreference
    ) {
    }

    private record LocationRestriction(Circle circle) {
    }

    private record Circle(Center center, int radius) {
    }

    private record Center(BigDecimal latitude, BigDecimal longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record NearbySearchResponse(List<GooglePlace> places) {
        private NearbySearchResponse {
            places = places == null ? List.of() : places;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GooglePlace(
            String id,
            LocalizedText displayName,
            String formattedAddress,
            Location location,
            BigDecimal rating,
            Integer userRatingCount,
            List<GooglePhoto> photos,
            String businessStatus,
            String primaryType,
            List<String> types
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
}
