// 동행 일정의 UTC 변환과 서머타임 경계를 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CompanionMeetingTimeConverterTest {
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Test
    void convertsParisWinterAndSummerUsingTheMeetingDate() {
        assertEquals(LocalDateTime.parse("2026-10-28T07:55:00"),
                CompanionMeetingTimeConverter.toUtc(LocalDateTime.parse("2026-10-28T08:55:00"), PARIS).orElseThrow());
        assertEquals(LocalDateTime.parse("2026-07-01T06:55:00"),
                CompanionMeetingTimeConverter.toUtc(LocalDateTime.parse("2026-07-01T08:55:00"), PARIS).orElseThrow());
        assertEquals(LocalDateTime.parse("2026-10-28T08:55:00"),
                CompanionMeetingTimeConverter.utcToLocal(LocalDateTime.parse("2026-10-28T07:55:00"), PARIS));
    }

    @ParameterizedTest
    @CsvSource({
            "Europe/Paris,2026-07-01T08:55:00,2026-07-01T06:55:00",
            "Europe/Paris,2026-10-28T08:55:00,2026-10-28T07:55:00",
            "Europe/Madrid,2026-07-01T08:55:00,2026-07-01T06:55:00",
            "Europe/Madrid,2026-10-28T08:55:00,2026-10-28T07:55:00",
            "Europe/London,2026-07-01T08:55:00,2026-07-01T07:55:00",
            "Europe/London,2026-10-28T08:55:00,2026-10-28T08:55:00",
            "UTC,2026-10-28T08:55:00,2026-10-28T08:55:00",
            "Europe/Paris,2026-07-01T00:15:00,2026-06-30T22:15:00"
    })
    void roundTripsLocalTimeAcrossSupportedZones(String zoneId, String local, String utc) {
        var zone = ZoneId.of(zoneId);
        var localTime = LocalDateTime.parse(local);
        var utcTime = LocalDateTime.parse(utc);
        assertEquals(utcTime, CompanionMeetingTimeConverter.toUtc(localTime, zone).orElseThrow());
        assertEquals(localTime, CompanionMeetingTimeConverter.utcToLocal(utcTime, zone));
    }

    @Test
    void doesNotInventAnInstantForNonexistentOrAmbiguousLocalTimes() {
        assertTrue(CompanionMeetingTimeConverter.toUtc(LocalDateTime.parse("2026-03-29T02:30:00"), PARIS).isEmpty());
        assertTrue(CompanionMeetingTimeConverter.toUtc(LocalDateTime.parse("2026-10-25T02:30:00"), PARIS).isEmpty());
    }
}
