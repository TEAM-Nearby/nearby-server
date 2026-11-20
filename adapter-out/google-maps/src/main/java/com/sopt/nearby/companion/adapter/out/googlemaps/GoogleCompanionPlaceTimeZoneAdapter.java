// Google Time Zone API로 장소 좌표의 IANA 시간대를 조회한다.
package com.sopt.nearby.companion.adapter.out.googlemaps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.companion.port.out.CompanionPlaceTimeZoneLookup;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleCompanionPlaceTimeZoneAdapter implements CompanionPlaceTimeZoneLookup {

    private static final Logger log = LoggerFactory.getLogger(GoogleCompanionPlaceTimeZoneAdapter.class);
    private static final URI TIME_ZONE_URI = URI.create("https://maps.googleapis.com/maps/api/timezone/json");

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final URI timeZoneUri;
    private final Duration timeout;
    // shortcut: 프로세스 로컬 캐시는 크기 제한이 없으며, 장소 수가 크게 늘면 TTL 캐시로 교체한다.
    private final Map<String, ZoneId> cache = new ConcurrentHashMap<>();

    @Autowired
    public GoogleCompanionPlaceTimeZoneAdapter(
            @Value("${nearby.google.time-zone.api-key:}") final String apiKey,
            @Value("${nearby.google.time-zone.timeout-ms:1000}") final long timeoutMs
    ) {
        this(HttpClient.newHttpClient(), new ObjectMapper(), apiKey, TIME_ZONE_URI, Duration.ofMillis(timeoutMs));
    }

    GoogleCompanionPlaceTimeZoneAdapter(
            final HttpClient client,
            final ObjectMapper mapper,
            final String apiKey,
            final URI timeZoneUri,
            final Duration timeout
    ) {
        this.client = client;
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.timeZoneUri = timeZoneUri;
        this.timeout = timeout;
    }

    @Override
    public Optional<ZoneId> findTimeZone(
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant
    ) {
        if (apiKey == null || apiKey.isBlank() || invalidCoordinates(latitude, longitude) || instant == null) {
            return Optional.empty();
        }
        String location = latitude.stripTrailingZeros().toPlainString()
                + "," + longitude.stripTrailingZeros().toPlainString();
        ZoneId cached = cache.get(location);
        if (cached != null) {
            return Optional.of(cached);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder(uri(location, instant)).timeout(timeout).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Google Time Zone lookup failed. status={}", response.statusCode());
                return Optional.empty();
            }
            JsonNode body = mapper.readTree(response.body());
            if (!"OK".equals(body.path("status").asText()) || !body.path("timeZoneId").isTextual()) {
                log.warn("Google Time Zone lookup returned no time zone. status={}", body.path("status").asText());
                return Optional.empty();
            }
            ZoneId zoneId = ZoneId.of(body.path("timeZoneId").asText());
            cache.put(location, zoneId);
            return Optional.of(zoneId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (IOException | IllegalArgumentException exception) {
            log.warn("Google Time Zone lookup failed. type={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    private URI uri(final String location, final Instant instant) {
        return URI.create(timeZoneUri + "?location=" + encode(location)
                + "&timestamp=" + instant.getEpochSecond() + "&key=" + encode(apiKey));
    }

    private boolean invalidCoordinates(final BigDecimal latitude, final BigDecimal longitude) {
        return latitude == null || longitude == null
                || latitude.abs().compareTo(BigDecimal.valueOf(90)) > 0
                || longitude.abs().compareTo(BigDecimal.valueOf(180)) > 0;
    }

    private String encode(final String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
