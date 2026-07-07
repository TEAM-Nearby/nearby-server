// Google Places 주변 혼밥 장소 검색 어댑터의 HTTP 요청과 응답 매핑을 검증한다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GoogleSoloDiningPlaceSearchAdapterTest {

    private HttpServer server;
    private GoogleSoloDiningPlaceSearchAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        adapter = new GoogleSoloDiningPlaceSearchAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "test-api-key",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/"),
                Duration.ofSeconds(1)
        );
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void sendsNearbySearchRequestAndMapsPlaces() {
        server.createContext("/v1/places:searchNearby", exchange -> {
            assertEquals("POST", exchange.getRequestMethod());
            assertEquals("test-api-key", exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            assertEquals(
                    "places.id,places.displayName,places.formattedAddress,places.location,places.rating,"
                            + "places.userRatingCount,places.photos,places.businessStatus,places.primaryType,"
                            + "places.types",
                    exchange.getRequestHeaders().getFirst("X-Goog-FieldMask")
            );
            String body = new String(exchange.getRequestBody().readAllBytes());
            assertEquals(true, body.contains("\"includedTypes\":[\"restaurant\",\"cafe\",\"pub\"]"));
            assertEquals(true, body.contains("\"maxResultCount\":20"));
            assertEquals(true, body.contains("\"radius\":1000"));
            respond(exchange, 200, """
                    {
                      "places": [
                        {
                          "id": "google-place-id",
                          "displayName": {"text": "니어바이 식당"},
                          "formattedAddress": "서울특별시 중구 세종대로 110",
                          "location": {"latitude": 37.56612, "longitude": 126.97845},
                          "rating": 4.3,
                          "userRatingCount": 22870,
                          "photos": [{"name": "places/google-place-id/photos/photo-resource"}],
                          "businessStatus": "OPERATIONAL",
                          "primaryType": "korean_restaurant",
                          "types": ["korean_restaurant", "restaurant", "food"]
                        },
                        {
                          "id": "google-cafe-id",
                          "displayName": {"text": "니어바이 카페"},
                          "location": {"latitude": 37.56622, "longitude": 126.97855},
                          "primaryType": "cafe",
                          "types": ["cafe", "food"]
                        }
                      ]
                    }
                    """);
        });

        List<SoloDiningPlaceSearchResult> result = adapter.search(new SoloDiningPlaceSearchRequest(
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                1000,
                20,
                List.of("restaurant", "cafe", "pub")
        ));

        assertEquals(2, result.size());
        assertEquals("google-place-id", result.get(0).googlePlaceId());
        assertEquals("니어바이 식당", result.get(0).name());
        assertEquals("서울특별시 중구 세종대로 110", result.get(0).address());
        assertEquals(SoloDiningPlaceCategory.RESTAURANT, result.get(0).category());
        assertEquals(new BigDecimal("4.3"), result.get(0).rating());
        assertEquals(22870, result.get(0).reviewCount());
        assertEquals("places/google-place-id/photos/photo-resource", result.get(0).photoReference());
        assertEquals(PlaceBusinessStatus.OPERATIONAL, result.get(0).businessStatus());
        assertEquals(SoloDiningPlaceCategory.CAFE, result.get(1).category());
        assertEquals(PlaceBusinessStatus.UNKNOWN, result.get(1).businessStatus());
    }

    @Test
    void mapsPubCategory() {
        server.createContext("/v1/places:searchNearby", exchange -> respond(exchange, 200, """
                {
                  "places": [
                    {
                      "id": "google-pub-id",
                      "displayName": {"text": "니어바이 펍"},
                      "location": {"latitude": 37.56612, "longitude": 126.97845},
                      "primaryType": "pub",
                      "types": ["pub", "food"]
                    }
                  ]
                }
                """));

        List<SoloDiningPlaceSearchResult> result = adapter.search(request());

        assertEquals(SoloDiningPlaceCategory.PUB, result.get(0).category());
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenGoogleReturnsError() {
        server.createContext("/v1/places:searchNearby", exchange -> respond(exchange, 500, "{}"));

        assertThrows(GooglePlaceApiException.class, () -> adapter.search(request()));
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenApiKeyIsMissing() {
        GoogleSoloDiningPlaceSearchAdapter adapterWithoutApiKey = new GoogleSoloDiningPlaceSearchAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/"),
                Duration.ofSeconds(1)
        );

        assertThrows(GooglePlaceApiException.class, () -> adapterWithoutApiKey.search(request()));
    }

    private SoloDiningPlaceSearchRequest request() {
        return new SoloDiningPlaceSearchRequest(
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                1000,
                20,
                List.of("restaurant")
        );
    }

    private static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
