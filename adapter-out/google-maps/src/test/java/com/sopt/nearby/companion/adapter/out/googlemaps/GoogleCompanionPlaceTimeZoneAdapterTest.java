// Google Time Zone API 응답과 좌표 캐시 동작을 검증한다.
package com.sopt.nearby.companion.adapter.out.googlemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.sopt.nearby.companion.application.CompanionPlaceTimeResolver;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GoogleCompanionPlaceTimeZoneAdapterTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void returnsAndCachesIanaTimeZoneForCoordinates() throws Exception {
        AtomicInteger requests = new AtomicInteger();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/timezone", exchange -> {
            requests.incrementAndGet();
            byte[] body = "{\"status\":\"OK\",\"timeZoneId\":\"Asia/Seoul\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        var adapter = adapter();

        var first = adapter.findTimeZone(new BigDecimal("37.566500"), new BigDecimal("126.978000"), Instant.EPOCH);
        var second = adapter.findTimeZone(new BigDecimal("37.5665"), new BigDecimal("126.978"), Instant.now());

        assertEquals(ZoneId.of("Asia/Seoul"), first.orElseThrow());
        assertEquals(first, second);
        assertEquals(1, requests.get());
    }

    @Test
    void rejectsInvalidCoordinatesWithoutCallingApi() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();

        assertTrue(adapter().findTimeZone(
                new BigDecimal("91"), BigDecimal.ZERO, Instant.EPOCH).isEmpty());
    }

    @Test
    void unknownTimeZoneIdFallsBackToUtc() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/timezone", exchange -> {
            byte[] body = "{\"status\":\"OK\",\"timeZoneId\":\"Unknown/Zone\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();

        var resolver = new CompanionPlaceTimeResolver(adapter());

        assertEquals("UTC", resolver.resolveZone(
                "unknown", new BigDecimal("37.5665"), new BigDecimal("126.978"), Instant.EPOCH).getId());
    }

    private GoogleCompanionPlaceTimeZoneAdapter adapter() {
        return new GoogleCompanionPlaceTimeZoneAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "test-key",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/timezone"),
                Duration.ofSeconds(1));
    }
}
