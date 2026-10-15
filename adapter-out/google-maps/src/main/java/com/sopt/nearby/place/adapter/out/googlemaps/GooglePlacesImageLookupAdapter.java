// Google Places API로 장소 대표 이미지 URL을 조회하는 어댑터다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GooglePlacesImageLookupAdapter implements PlaceImageLookupPort {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesImageLookupAdapter.class);
    private static final URI GOOGLE_PLACES_BASE_URI = URI.create("https://places.googleapis.com/v1/");
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final URI baseUri;
    private final int maxWidthPx;
    private final Duration timeout;

    @Autowired
    public GooglePlacesImageLookupAdapter(
            @Value("${nearby.google.places.api-key:}") final String apiKey,
            @Value("${nearby.google.places.photo.max-width-px:640}") final int maxWidthPx,
            @Value("${nearby.google.places.timeout-ms:1000}") final long timeoutMs
    ) {
        this(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                apiKey,
                GOOGLE_PLACES_BASE_URI,
                maxWidthPx,
                Duration.ofMillis(timeoutMs)
        );
    }

    GooglePlacesImageLookupAdapter(
            final HttpClient httpClient,
            final ObjectMapper objectMapper,
            final String apiKey,
            final URI baseUri,
            final int maxWidthPx,
            final Duration timeout
    ) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.baseUri = baseUri;
        this.maxWidthPx = maxWidthPx;
        this.timeout = timeout;
    }

    @Override
    public Optional<ResolvedPlaceImage> findImage(final String googlePlaceId, final String photoReference) {
        if (isBlank(apiKey)) {
            return Optional.empty();
        }
        if (!isBlank(photoReference)) {
            return findByPhotoName(photoReference, List.of());
        }
        if (isBlank(googlePlaceId)) {
            return Optional.empty();
        }
        return findFirstPhoto(googlePlaceId)
                .flatMap(photo -> findByPhotoName(photo.name(), toAttributions(photo.authorAttributions())));
    }

    private Optional<ResolvedPlaceImage> findByPhotoName(
            final String photoName,
            final List<ResolvedPlaceImage.ImageAttribution> attributions
    ) {
        return requestPhotoUri(photoName)
                .map(photoUri -> new ResolvedPlaceImage(
                        photoUri,
                        ResolvedPlaceImage.GOOGLE_MAPS,
                        attributions
                ));
    }

    private Optional<String> requestPhotoUri(final String photoName) {
        if (isBlank(photoName)) {
            return Optional.empty();
        }

        URI uri = baseUri.resolve(photoName + "/media?maxWidthPx=" + maxWidthPx
                + "&skipHttpRedirect=true&key=" + queryParam(apiKey));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET()
                .build();

        return send(request)
                .flatMap(body -> read(body, PhotoMediaResponse.class))
                .map(PhotoMediaResponse::photoUri)
                .filter(value -> !isBlank(value));
    }

    private Optional<PlacePhoto> findFirstPhoto(final String googlePlaceId) {
        URI uri = baseUri.resolve("places/" + pathSegment(googlePlaceId));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "photos")
                .GET()
                .build();

        return send(request)
                .flatMap(body -> read(body, PlaceDetailsResponse.class))
                .map(PlaceDetailsResponse::photos)
                .filter(photos -> !photos.isEmpty())
                .map(List::getFirst);
    }

    private Optional<String> send(final HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < HTTP_OK_MIN || response.statusCode() > HTTP_OK_MAX) {
                return Optional.empty();
            }
            return Optional.of(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            log.debug("Google Places image lookup failed.", exception);
            return Optional.empty();
        }
    }

    private <T> Optional<T> read(final String body, final Class<T> type) {
        try {
            return Optional.of(objectMapper.readValue(body, type));
        } catch (IOException exception) {
            log.debug("Google Places response parsing failed.", exception);
            return Optional.empty();
        }
    }

    private List<ResolvedPlaceImage.ImageAttribution> toAttributions(final List<AuthorAttribution> attributions) {
        if (attributions == null) {
            return List.of();
        }
        return attributions.stream()
                .map(attribution -> new ResolvedPlaceImage.ImageAttribution(
                        attribution.displayName(),
                        attribution.uri(),
                        attribution.photoUri()
                ))
                .toList();
    }

    private String queryParam(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String pathSegment(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlaceDetailsResponse(List<PlacePhoto> photos) {
        private PlaceDetailsResponse {
            photos = photos == null ? List.of() : photos;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PlacePhoto(String name, List<AuthorAttribution> authorAttributions) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthorAttribution(String displayName, String uri, String photoUri) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record PhotoMediaResponse(String photoUri) {
    }
}
