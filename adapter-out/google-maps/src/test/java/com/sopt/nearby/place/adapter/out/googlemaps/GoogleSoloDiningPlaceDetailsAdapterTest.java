// Google Places 상세 조회 어댑터의 HTTP 요청과 응답 매핑을 검증한다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
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

class GoogleSoloDiningPlaceDetailsAdapterTest {

    private HttpServer server;
    private GoogleSoloDiningPlaceDetailsAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        adapter = new GoogleSoloDiningPlaceDetailsAdapter(
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
    void sendsPlaceDetailsRequestAndMapsPlace() {
        server.createContext("/v1/places/google-place-id", exchange -> {
            assertEquals("GET", exchange.getRequestMethod());
            assertEquals("languageCode=ko&regionCode=KR", exchange.getRequestURI().getQuery());
            assertEquals("test-api-key", exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            assertEquals(
                    "id,displayName,formattedAddress,location,primaryType,types,rating,userRatingCount,"
                            + "nationalPhoneNumber,photos,businessStatus,priceLevel,priceRange,"
                            + "regularOpeningHours,editorialSummary",
                    exchange.getRequestHeaders().getFirst("X-Goog-FieldMask")
            );
            respond(exchange, 200, """
                    {
                      "id": "google-place-id",
                      "displayName": {"text": "르상크 바르셀로나점"},
                      "formattedAddress": "서울특별시 중구 세종대로 110",
                      "location": {"latitude": 37.56612, "longitude": 126.97845},
                      "primaryType": "cafe",
                      "types": ["cafe", "food"],
                      "rating": 4.3,
                      "userRatingCount": 22870,
                      "nationalPhoneNumber": "02-1234-5678",
                      "photos": [
                        {"name": "places/google-place-id/photos/photo-1"},
                        {"name": "places/google-place-id/photos/photo-2"}
                      ],
                      "businessStatus": "OPERATIONAL",
                      "priceLevel": "PRICE_LEVEL_MODERATE",
                      "priceRange": {
                        "startPrice": {"currencyCode": "KRW", "units": "10000"},
                        "endPrice": {"currencyCode": "KRW", "units": "20000"}
                      },
                      "regularOpeningHours": {
                        "weekdayDescriptions": ["월요일: 오전 11:00~오후 9:00"]
                      },
                      "editorialSummary": {"text": "혼밥하기 좋은 조용한 식당입니다."}
                    }
                    """);
        });

        SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

        assertEquals("google-place-id", result.googlePlaceId());
        assertEquals("르상크 바르셀로나점", result.name());
        assertEquals("서울특별시 중구 세종대로 110", result.address());
        assertEquals(new BigDecimal("37.56612"), result.latitude());
        assertEquals(new BigDecimal("126.97845"), result.longitude());
        assertEquals(SoloDiningPlaceCategory.CAFE, result.category());
        assertEquals(new BigDecimal("4.3"), result.rating());
        assertEquals(22870, result.reviewCount());
        assertEquals("02-1234-5678", result.phoneNumber());
        assertEquals("places/google-place-id/photos/photo-1", result.photoReference());
        assertEquals(List.of(
                "places/google-place-id/photos/photo-1",
                "places/google-place-id/photos/photo-2"
        ), result.photoReferences());
        assertEquals(PlaceBusinessStatus.OPERATIONAL, result.businessStatus());
        assertEquals("PRICE_LEVEL_MODERATE", result.priceLevel());
        assertEquals("₩10,000~₩20,000", result.priceRange());
        assertEquals(List.of("월요일: 오전 11:00~오후 9:00"), result.regularOpeningHours());
        assertEquals("혼밥하기 좋은 조용한 식당입니다.", result.editorialSummary());
    }

    @Test
    void mapsUnboundedPriceRangeAndUnspecifiedPriceLevel() {
        server.createContext("/v1/places/google-place-id", exchange -> respond(exchange, 200, """
                {
                  "id": "google-place-id",
                  "displayName": {"text": "니어바이 식당"},
                  "location": {"latitude": 37.56612, "longitude": 126.97845},
                  "primaryType": "museum",
                  "priceLevel": "PRICE_LEVEL_UNSPECIFIED",
                  "priceRange": {
                    "startPrice": {"currencyCode": "KRW", "units": "10000"}
                  }
                }
                """));

        SoloDiningPlaceDetailsResult result = adapter.findByGooglePlaceId("google-place-id");

        assertNull(result.category());
        assertNull(result.priceLevel());
        assertEquals("₩10,000 이상", result.priceRange());
    }

    @Test
    void mapsCoffeeShopAndBarTypes() {
        server.createContext("/v1/places/coffee-place-id", exchange -> respond(exchange, 200, """
                {
                  "id": "coffee-place-id",
                  "displayName": {"text": "커피집"},
                  "location": {"latitude": 37.56612, "longitude": 126.97845},
                  "primaryType": "coffee_shop",
                  "types": ["coffee_shop", "food"]
                }
                """));
        server.createContext("/v1/places/bar-place-id", exchange -> respond(exchange, 200, """
                {
                  "id": "bar-place-id",
                  "displayName": {"text": "바"},
                  "location": {"latitude": 37.56612, "longitude": 126.97845},
                  "primaryType": "bar",
                  "types": ["bar", "food"]
                }
                """));

        SoloDiningPlaceDetailsResult coffee = adapter.findByGooglePlaceId("coffee-place-id");
        SoloDiningPlaceDetailsResult bar = adapter.findByGooglePlaceId("bar-place-id");

        assertEquals(SoloDiningPlaceCategory.CAFE, coffee.category());
        assertEquals(SoloDiningPlaceCategory.PUB, bar.category());
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenMoneyUnitsAreInvalid() {
        server.createContext("/v1/places/google-place-id", exchange -> respond(exchange, 200, """
                {
                  "id": "google-place-id",
                  "displayName": {"text": "니어바이 식당"},
                  "location": {"latitude": 37.56612, "longitude": 126.97845},
                  "priceRange": {
                    "startPrice": {"currencyCode": "KRW", "units": "not-number"}
                  }
                }
                """));

        assertThrows(GooglePlaceApiException.class, () -> adapter.findByGooglePlaceId("google-place-id"));
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenGoogleReturnsError() {
        server.createContext("/v1/places/google-place-id", exchange -> respond(exchange, 500, "{}"));

        assertThrows(GooglePlaceApiException.class, () -> adapter.findByGooglePlaceId("google-place-id"));
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenApiKeyIsMissing() {
        GoogleSoloDiningPlaceDetailsAdapter adapterWithoutApiKey = new GoogleSoloDiningPlaceDetailsAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/"),
                Duration.ofSeconds(1)
        );

        assertThrows(GooglePlaceApiException.class, () -> adapterWithoutApiKey.findByGooglePlaceId("google-place-id"));
    }

    private static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
