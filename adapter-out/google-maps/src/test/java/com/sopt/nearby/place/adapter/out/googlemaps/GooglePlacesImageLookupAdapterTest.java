// Google Places 대표 이미지 조회 어댑터의 HTTP 요청과 fallback을 검증한다.
package com.sopt.nearby.place.adapter.out.googlemaps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GooglePlacesImageLookupAdapterTest {

    private HttpServer server;
    private GooglePlacesImageLookupAdapter adapter;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        adapter = new GooglePlacesImageLookupAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "test-api-key",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/"),
                640,
                Duration.ofSeconds(1)
        );
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void returnsPhotoUriWhenPhotoReferenceIsPhotoResourceName() {
        server.createContext("/v1/places/google-place-id/photos/photo-resource/media", exchange -> {
            assertEquals("maxWidthPx=640&skipHttpRedirect=true&key=test-api-key", exchange.getRequestURI().getQuery());
            respond(exchange, 200, """
                    {"photoUri":"https://lh3.googleusercontent.com/photo.jpg"}
                    """);
        });

        Optional<ResolvedPlaceImage> result = adapter.findImage(
                "google-place-id",
                "places/google-place-id/photos/photo-resource"
        );

        assertTrue(result.isPresent());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", result.get().imageUrl());
        assertEquals("GOOGLE_MAPS", result.get().imageSource());
        assertEquals(0, result.get().imageAttributions().size());
    }

    @Test
    void resolvesFirstPhotoFromPlaceDetailsWhenPhotoReferenceIsMissing() {
        server.createContext("/v1/places/google-place-id", exchange -> {
            assertEquals("test-api-key", exchange.getRequestHeaders().getFirst("X-Goog-Api-Key"));
            assertEquals("photos", exchange.getRequestHeaders().getFirst("X-Goog-FieldMask"));
            respond(exchange, 200, """
                    {
                      "photos": [
                        {
                          "name": "places/google-place-id/photos/photo-resource",
                          "authorAttributions": [
                            {
                              "displayName": "Google User",
                              "uri": "https://maps.google.com/contrib/1",
                              "photoUri": "https://lh3.googleusercontent.com/profile.jpg"
                            }
                          ]
                        }
                      ]
                    }
                    """);
        });
        server.createContext("/v1/places/google-place-id/photos/photo-resource/media", exchange -> respond(exchange, 200, """
                {"photoUri":"https://lh3.googleusercontent.com/photo.jpg"}
                """));

        Optional<ResolvedPlaceImage> result = adapter.findImage("google-place-id", null);

        assertTrue(result.isPresent());
        assertEquals("https://lh3.googleusercontent.com/photo.jpg", result.get().imageUrl());
        assertEquals("Google User", result.get().imageAttributions().get(0).displayName());
    }

    @Test
    void returnsEmptyWhenApiKeyIsMissing() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        server.createContext("/v1/places/google-place-id", exchange -> {
            requests.incrementAndGet();
            respond(exchange, 200, "{}");
        });
        GooglePlacesImageLookupAdapter adapterWithoutApiKey = new GooglePlacesImageLookupAdapter(
                HttpClient.newHttpClient(),
                new ObjectMapper(),
                "",
                URI.create("http://localhost:" + server.getAddress().getPort() + "/v1/"),
                640,
                Duration.ofSeconds(1)
        );

        Optional<ResolvedPlaceImage> result = adapterWithoutApiKey.findImage("google-place-id", null);

        assertTrue(result.isEmpty());
        assertEquals(0, requests.get());
    }

    @Test
    void returnsEmptyWhenGoogleReturnsError() {
        server.createContext("/v1/places/google-place-id/photos/photo-resource/media",
                exchange -> respond(exchange, 500, "{}"));

        Optional<ResolvedPlaceImage> result = adapter.findImage(
                "google-place-id",
                "places/google-place-id/photos/photo-resource"
        );

        assertTrue(result.isEmpty());
    }

    private static void respond(final HttpExchange exchange, final int status, final String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
