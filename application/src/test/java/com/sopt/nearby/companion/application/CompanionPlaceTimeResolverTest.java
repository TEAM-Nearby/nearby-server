// 장소 좌표와 주소에 따른 IANA 시간대 해석 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.port.out.CompanionPlaceTimeZoneLookup;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CompanionPlaceTimeResolverTest {

    private static final Instant NOW = Instant.parse("2026-11-20T01:41:00Z");

    @Test
    void unsupportedSeoulUsesCoordinateTimeZoneAndKeepsCityNull() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver(
                lookup(ZoneId.of("Asia/Seoul")));

        var result = resolver.resolveCurrentTime(
                "서울특별시 종로구", new BigDecimal("37.5665"), new BigDecimal("126.9780"), NOW);

        assertNull(result.city());
        assertEquals("Asia/Seoul", result.currentLocalTime().getZone().getId());
        assertEquals(10, result.currentLocalTime().getHour());
    }

    @Test
    void japaneseAddressFallsBackToTokyoWhenLookupFails() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver(
                (latitude, longitude, instant) -> Optional.empty());

        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "日本 大阪府大阪市", new BigDecimal("34.6937"), new BigDecimal("135.5023"), NOW));
    }

    @Test
    void koreanAddressWithoutCountryFallsBackToSeoulWhenLookupFails() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver(
                (latitude, longitude, instant) -> Optional.empty());

        assertEquals(ZoneId.of("Asia/Seoul"), resolver.resolveZone(
                "서울특별시 종로구", new BigDecimal("37.5665"), new BigDecimal("126.9780"), NOW));
    }

    @Test
    void supportedCityWinsOverCoordinateLookupAndKeepsDst() {
        AtomicInteger calls = new AtomicInteger();
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver((latitude, longitude, instant) -> {
            calls.incrementAndGet();
            return Optional.of(ZoneId.of("Asia/Seoul"));
        });

        var result = resolver.resolveCurrentTime(
                "Madrid, Spain", new BigDecimal("40.4168"), new BigDecimal("-3.7038"),
                Instant.parse("2026-07-01T12:00:00Z"));

        assertEquals("Europe/Madrid", result.currentLocalTime().getZone().getId());
        assertEquals(2 * 60 * 60, result.currentLocalTime().getOffset().getTotalSeconds());
        assertEquals(0, calls.get());
    }

    @Test
    void missingCoordinatesAndUnknownAddressFallBackToUtc() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver(lookup(ZoneId.of("Asia/Seoul")));

        assertEquals("UTC", resolver.resolveZone("unknown", null, null, NOW).getId());
    }

    private CompanionPlaceTimeZoneLookup lookup(final ZoneId zoneId) {
        return (latitude, longitude, instant) -> Optional.of(zoneId);
    }
}
