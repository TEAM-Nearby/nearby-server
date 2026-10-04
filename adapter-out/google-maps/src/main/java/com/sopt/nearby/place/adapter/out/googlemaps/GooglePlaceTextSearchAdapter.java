// Google Text Search에 검색어·표시 언어·위치 우선순위를 전달한다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.exception.PlaceSearchFailedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchRateLimitedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchTimeoutException;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Attribution;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Category;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Place;
import com.sopt.nearby.place.port.out.PlaceTextSearchPort;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GooglePlaceTextSearchAdapter implements PlaceTextSearchPort {
    private static final Logger log = LoggerFactory.getLogger(GooglePlaceTextSearchAdapter.class);
    private static final URI SEARCH_URI = URI.create("https://places.googleapis.com/v1/places:searchText");
    private static final String FIELD_MASK = "places.id,places.displayName,places.formattedAddress,"
            + "places.location,places.primaryType,places.types,places.attributions,nextPageToken";
    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final URI searchUri;
    private final Duration timeout;

    @Autowired
    public GooglePlaceTextSearchAdapter(
            @Value("${nearby.google.places.api-key:}") final String apiKey,
            @Value("${nearby.google.places.search-timeout-ms:5000}") final long timeoutMs
    ) {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs)).build(),
                new ObjectMapper(), apiKey, SEARCH_URI, Duration.ofMillis(timeoutMs));
    }

    GooglePlaceTextSearchAdapter(final HttpClient client, final ObjectMapper mapper, final String apiKey,
                                final URI searchUri, final Duration timeout) {
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.searchUri = searchUri;
        this.timeout = timeout;
    }

    @Override
    public PlaceSearchPage search(final SearchPlacesCommand command) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new PlaceSearchFailedException();
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(searchUri).timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", FIELD_MASK)
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body(command))))
                    .build();
            HttpResponse<String> response = send(request);
            if (response.statusCode() / 100 != 2) {
                log.warn("Google Places search failed. status={}", response.statusCode());
            }
            if (response.statusCode() == 429) {
                throw new PlaceSearchRateLimitedException();
            }
            if (response.statusCode() / 100 != 2) {
                throw new PlaceSearchFailedException();
            }
            return toPage(mapper.readTree(response.body()));
        } catch (HttpTimeoutException exception) {
            log.warn("Google Places search timed out. type={}", exception.getClass().getSimpleName());
            throw new PlaceSearchTimeoutException();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Google Places search interrupted. type={}", exception.getClass().getSimpleName());
            throw new PlaceSearchFailedException();
        } catch (IOException | IllegalArgumentException exception) {
            log.warn("Google Places search error. type={}", exception.getClass().getSimpleName());
            throw new PlaceSearchFailedException();
        }
    }

    private HttpResponse<String> send(final HttpRequest request) throws IOException, InterruptedException {
        var response = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        try {
            return response.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (TimeoutException exception) {
            response.cancel(true);
            throw new HttpTimeoutException("Google Places search response timed out");
        } catch (InterruptedException exception) {
            response.cancel(true);
            throw exception;
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof IOException ioException) {
                throw ioException;
            }
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IOException("Google Places search request failed", cause);
        }
    }

    private Map<String, Object> body(final SearchPlacesCommand command) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("textQuery", command.query());
        body.put("languageCode", command.languageCode());
        body.put("pageSize", command.pageSize());
        body.put("locationBias", Map.of("circle", Map.of(
                "center", Map.of("latitude", command.latitude(), "longitude", command.longitude()),
                "radius", command.radiusMeters())));
        if (command.pageToken() != null) {
            body.put("pageToken", command.pageToken());
        }
        return body;
    }

    private PlaceSearchPage toPage(final JsonNode root) {
        if (root == null || !root.isObject() || root.has("error")) {
            throw new PlaceSearchFailedException();
        }
        Map<String, Place> places = new LinkedHashMap<>();
        if (root.has("places")) {
            JsonNode items = root.get("places");
            if (!items.isArray()) {
                throw new PlaceSearchFailedException();
            }
            for (JsonNode item : items) {
                Place place = new Place(requiredText(item, "id"), requiredText(item.path("displayName"), "text"),
                        optionalText(item, "formattedAddress"), coordinate(item.path("location"), "latitude", 90),
                        coordinate(item.path("location"), "longitude", 180), category(item), attributions(item));
                places.putIfAbsent(place.googlePlaceId(), place);
            }
        }
        return new PlaceSearchPage(List.copyOf(places.values()), optionalText(root, "nextPageToken"));
    }

    private Category category(final JsonNode item) {
        Category primary = categoryOf(optionalText(item, "primaryType"));
        if (primary != Category.OTHER) {
            return primary;
        }
        for (JsonNode type : item.path("types")) {
            Category category = categoryOf(type.asText());
            if (category != Category.OTHER) {
                return category;
            }
        }
        return Category.OTHER;
    }

    private Category categoryOf(final String type) {
        if (type == null) {
            return Category.OTHER;
        }
        if (type.equals("cafe") || type.endsWith("_cafe") || type.equals("coffee_shop")) {
            return Category.CAFE;
        }
        if (type.equals("pub") || type.equals("bar") || type.endsWith("_bar")) {
            return Category.PUB;
        }
        if (type.equals("restaurant") || type.endsWith("_restaurant")) {
            return Category.RESTAURANT;
        }
        return switch (type) {
            case "museum" -> Category.MUSEUM;
            case "tourist_attraction", "historical_landmark", "observation_deck" -> Category.PHOTO_SPOT;
            default -> Category.OTHER;
        };
    }

    private List<Attribution> attributions(final JsonNode item) {
        List<Attribution> result = new ArrayList<>();
        if (item.has("attributions")) {
            if (!item.get("attributions").isArray()) {
                throw new PlaceSearchFailedException();
            }
            for (JsonNode attribution : item.get("attributions")) {
                result.add(new Attribution(requiredText(attribution, "provider"),
                        optionalText(attribution, "providerUri")));
            }
        }
        return result;
    }

    private String requiredText(final JsonNode node, final String field) {
        String value = optionalText(node, field);
        if (value == null) {
            throw new PlaceSearchFailedException();
        }
        return value;
    }

    private String optionalText(final JsonNode node, final String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isTextual()) {
            throw new PlaceSearchFailedException();
        }
        return value.textValue().isBlank() ? null : value.textValue();
    }

    private BigDecimal coordinate(final JsonNode location, final String field, final int limit) {
        JsonNode value = location.path(field);
        if (!value.isNumber() || value.decimalValue().abs().compareTo(BigDecimal.valueOf(limit)) > 0) {
            throw new PlaceSearchFailedException();
        }
        return value.decimalValue();
    }
}
