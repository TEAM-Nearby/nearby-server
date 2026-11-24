// 장소 좌표와 주소에 따른 IANA 시간대 해석 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CompanionPlaceTimeResolverTest {

    private static final Instant NOW = Instant.parse("2026-11-20T01:41:00Z");

    @Test
    void unsupportedSeoulUsesSeoulTimeZoneAndKeepsCityNull() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        var result = resolver.resolveCurrentTime(
                "서울특별시 종로구", new BigDecimal("37.5665"), new BigDecimal("126.9780"), NOW);

        assertNull(result.city());
        assertEquals("Asia/Seoul", result.currentLocalTime().getZone().getId());
        assertEquals(10, result.currentLocalTime().getHour());
    }

    @Test
    void japaneseAddressUsesTokyo() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "日本 大阪府大阪市", new BigDecimal("34.6937"), new BigDecimal("135.5023"), NOW));
    }

    @Test
    void japaneseAddressWithoutCountryUsesTokyo() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "도쿄도 시부야구", new BigDecimal("35.6595"), new BigDecimal("139.7005"), NOW));
    }

    @Test
    void distinguishesCountryFromPlaceNameAndRecognizesFukuoka() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        assertEquals(ZoneId.of("Asia/Seoul"), resolver.resolveZone(
                "Japan Center, 1737 Post St, San Francisco, CA, USA", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Seoul"), resolver.resolveZone(
                "Japan Center, Toronto, Canada", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Seoul"), resolver.resolveZone(
                "Restaurant Japan, Toronto, Canada", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "Fukuoka, Japan", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "1-1 Shibuya, Tokyo 150-0002 Japan", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "Japan, 1-1 Shibuya, Tokyo 150-0002", null, null, NOW));
        assertEquals(ZoneId.of("Asia/Tokyo"), resolver.resolveZone(
                "후쿠오카현 후쿠오카시 하카타구", null, null, NOW));
    }

    @Test
    void koreanAddressWithoutCountryUsesSeoul() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        assertEquals(ZoneId.of("Asia/Seoul"), resolver.resolveZone(
                "서울특별시 종로구", new BigDecimal("37.5665"), new BigDecimal("126.9780"), NOW));
    }

    @Test
    void supportedCityKeepsDst() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        var result = resolver.resolveCurrentTime(
                "Madrid, Spain", new BigDecimal("40.4168"), new BigDecimal("-3.7038"),
                Instant.parse("2026-07-01T12:00:00Z"));

        assertEquals("Europe/Madrid", result.currentLocalTime().getZone().getId());
        assertEquals(2 * 60 * 60, result.currentLocalTime().getOffset().getTotalSeconds());
    }

    @Test
    void missingCoordinatesAndUnknownAddressFallBackToSeoul() {
        CompanionPlaceTimeResolver resolver = new CompanionPlaceTimeResolver();

        assertEquals("Asia/Seoul", resolver.resolveZone("unknown", null, null, NOW).getId());
    }
}
