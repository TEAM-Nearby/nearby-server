// 실제 HTTP 경계에서 검색 조건·응답 매핑·외부 오류 처리를 검증한다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.exception.PlaceSearchFailedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchRateLimitedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchTimeoutException;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Category;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class GooglePlaceTextSearchAdapterTest {
    private HttpServer server;
    private URI uri;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/v1/places:searchText");
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"시우다드 콘달", "Ciutat Comtal", "Ciudad Condal"})
    void sendsTextAndChosenLocationWithoutKoreaRegionOrRestaurantFilter(final String query) {
        AtomicReference<JsonNode> received = new AtomicReference<>();
        AtomicReference<String> mask = new AtomicReference<>();
        AtomicReference<String> key = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        server.createContext("/v1/places:searchText", exchange -> {
            received.set(mapper.readTree(exchange.getRequestBody()));
            mask.set(exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
            key.set(exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            method.set(exchange.getRequestMethod());
            respond(exchange, 200, """
                    {"places":[{"id":"target-id","displayName":{"text":"시우다드 콘달"},
                     "formattedAddress":"Barcelona, Spain","location":{"latitude":41.389,"longitude":2.165},
                     "primaryType":"spanish_restaurant","unknownField":true,
                     "attributions":[{"provider":"Example provider","providerUri":"https://example.com"}]}],
                     "nextPageToken":"next-token"}
                    """);
        });
        var page = adapter(Duration.ofSeconds(2)).search(command(query, "ko", "page-token"));
        assertEquals("POST", method.get());
        assertEquals("test-key", key.get());
        assertEquals("places.id,places.displayName,places.formattedAddress,places.location,"
                + "places.primaryType,places.types,places.attributions,nextPageToken", mask.get());
        JsonNode body = received.get();
        assertEquals(query, body.path("textQuery").asText());
        assertEquals("ko", body.path("languageCode").asText());
        assertEquals(20, body.path("pageSize").asInt());
        assertEquals("page-token", body.path("pageToken").asText());
        assertEquals(41.3874, body.at("/locationBias/circle/center/latitude").asDouble());
        assertEquals(2.1686, body.at("/locationBias/circle/center/longitude").asDouble());
        assertEquals(20_000, body.at("/locationBias/circle/radius").asInt());
        assertFalse(body.has("regionCode"));
        assertFalse(body.has("includedType"));
        assertFalse(body.has("locationRestriction"));
        assertEquals("next-token", page.nextPageToken());
        var place = page.places().getFirst();
        assertEquals("target-id", place.googlePlaceId());
        assertEquals("시우다드 콘달", place.name());
        assertEquals("Barcelona, Spain", place.address());
        assertEquals(new BigDecimal("41.389"), place.latitude());
        assertEquals(Category.RESTAURANT, place.category());
        assertEquals("https://example.com", place.attributions().getFirst().providerUri());
    }

    @ParameterizedTest
    @CsvSource({"cafe,CAFE", "pub,PUB", "museum,MUSEUM", "tourist_attraction,PHOTO_SPOT", "store,OTHER"})
    void mapsCategoriesWithoutRestrictingSearchToFood(final String type, final Category expected) {
        server.createContext("/v1/places:searchText", exchange -> respond(exchange, 200,
                """
                {"places":[{"id":"id","displayName":{"text":"place"},
                  "location":{"latitude":0,"longitude":0},"primaryType":"%s"}]}
                """.formatted(type)));
        var place = adapter(Duration.ofSeconds(2)).search(command("장소", "en", null)).places().getFirst();
        assertEquals(expected, place.category());
        assertNull(place.address());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"places\":[]}"})
    void treatsOnlyValidEmptyResponsesAsNoResults(final String body) {
        server.createContext("/v1/places:searchText", exchange -> respond(exchange, 200, body));
        var result = adapter(Duration.ofSeconds(2)).search(command("없는 장소", "ko", null));
        assertTrue(result.places().isEmpty());
        assertNull(result.nextPageToken());
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "[]", "not-json", "{\"places\":null}", "{\"places\":[null]}",
            "{\"places\":[{}]}", "{\"error\":{}}", "{\"places\":[{\"id\":1}]}",
            "{\"places\":[{\"id\":\"id\",\"displayName\":{\"text\":\"n\"},\"location\":{\"latitude\":91,\"longitude\":0}}]}"})
    void rejectsMalformedResponsesInsteadOfReportingNoResults(final String body) {
        server.createContext("/v1/places:searchText", exchange -> respond(exchange, 200, body));
        assertThrows(PlaceSearchFailedException.class,
                () -> adapter(Duration.ofSeconds(2)).search(command("장소", "ko", null)));
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 500, 503})
    void reportsUpstreamFailure(final int status) {
        server.createContext("/v1/places:searchText", exchange -> respond(exchange, status, "{}"));
        assertThrows(PlaceSearchFailedException.class,
                () -> adapter(Duration.ofSeconds(2)).search(command("장소", "ko", null)));
    }

    @Test
    void distinguishesQuotaExhaustion() {
        server.createContext("/v1/places:searchText", exchange -> respond(exchange, 429, "{}"));
        assertThrows(PlaceSearchRateLimitedException.class,
                () -> adapter(Duration.ofSeconds(2)).search(command("장소", "ko", null)));
    }

    @Test
    void distinguishesTimeout() {
        server.createContext("/v1/places:searchText", exchange -> {
            try {
                Thread.sleep(300);
                respond(exchange, 200, "{}");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        assertThrows(PlaceSearchTimeoutException.class,
                () -> adapter(Duration.ofMillis(100)).search(command("장소", "ko", null)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "{"})
    void timesOutAndCancelsWhileResponseBodyIsStalled(final String prefix) throws Exception {
        CountDownLatch headersSent = new CountDownLatch(1);
        CountDownLatch releaseBody = new CountDownLatch(1);
        stallResponseBody(prefix, headersSent, releaseBody);
        HttpClient client = HttpClient.newHttpClient();
        var adapter = new GooglePlaceTextSearchAdapter(client, mapper, "test-key", uri, Duration.ofSeconds(1));
        var search = new FutureTask<>(() -> assertThrows(PlaceSearchTimeoutException.class,
                () -> adapter.search(command("장소", "ko", null))));
        Thread worker = Thread.ofVirtual().start(search);
        try {
            assertTrue(headersSent.await(3, TimeUnit.SECONDS));
            search.get(3, TimeUnit.SECONDS);
            client.shutdown();
            assertTrue(client.awaitTermination(Duration.ofSeconds(2)),
                    "본문 전송이 멈춰 있어도 시간 초과한 HTTP 요청은 정리되어야 한다.");
        } finally {
            releaseBody.countDown();
            worker.interrupt();
            worker.join(3000);
            client.shutdownNow();
        }
    }

    @Test
    void cancelsStalledRequestAndPreservesInterruptFlag() throws Exception {
        CountDownLatch headersSent = new CountDownLatch(1);
        CountDownLatch releaseBody = new CountDownLatch(1);
        stallResponseBody("{", headersSent, releaseBody);
        HttpClient client = HttpClient.newHttpClient();
        var adapter = new GooglePlaceTextSearchAdapter(client, mapper, "test-key", uri, Duration.ofSeconds(10));
        var search = new FutureTask<>(() -> {
            assertThrows(PlaceSearchFailedException.class, () -> adapter.search(command("장소", "ko", null)));
            return Thread.currentThread().isInterrupted();
        });
        Thread worker = Thread.ofVirtual().start(search);
        try {
            assertTrue(headersSent.await(3, TimeUnit.SECONDS));
            worker.interrupt();
            assertTrue(search.get(3, TimeUnit.SECONDS));
            client.shutdown();
            assertTrue(client.awaitTermination(Duration.ofSeconds(2)),
                    "인터럽트가 발생한 HTTP 요청은 정리되어야 한다.");
        } finally {
            releaseBody.countDown();
            worker.interrupt();
            worker.join(3000);
            client.shutdownNow();
        }
    }

    @Test
    void reportsFailureWhenResponseBodyIsTruncated() {
        server.createContext("/v1/places:searchText", exchange -> {
            exchange.sendResponseHeaders(200, 10);
            exchange.getResponseBody().write("{}".getBytes(StandardCharsets.UTF_8));
            exchange.close();
        });
        assertThrows(PlaceSearchFailedException.class,
                () -> adapter(Duration.ofSeconds(2)).search(command("장소", "ko", null)));
    }

    @Test
    void failsForMissingKeyWithoutCallingGoogle() {
        var adapter = new GooglePlaceTextSearchAdapter(HttpClient.newHttpClient(), mapper, "", uri, Duration.ofSeconds(1));
        assertThrows(PlaceSearchFailedException.class, () -> adapter.search(command("장소", "ko", null)));
    }

    private GooglePlaceTextSearchAdapter adapter(final Duration timeout) {
        return new GooglePlaceTextSearchAdapter(HttpClient.newHttpClient(), mapper, "test-key", uri, timeout);
    }

    private SearchPlacesCommand command(final String query, final String language, final String token) {
        return new SearchPlacesCommand(query, new BigDecimal("41.3874"), new BigDecimal("2.1686"),
                20_000, language, 20, token);
    }

    private void stallResponseBody(final String prefix, final CountDownLatch headersSent,
                                   final CountDownLatch releaseBody) {
        server.createContext("/v1/places:searchText", exchange -> {
            try {
                exchange.getRequestBody().readAllBytes();
                exchange.sendResponseHeaders(200, 2);
                exchange.getResponseBody().write(prefix.getBytes(StandardCharsets.UTF_8));
                exchange.getResponseBody().flush();
                headersSent.countDown();
                releaseBody.await(10, TimeUnit.SECONDS);
                exchange.getResponseBody().write("{}".substring(prefix.length()).getBytes(StandardCharsets.UTF_8));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
    }

    private static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
