// UTC Clock 설정을 검증한다.
package com.sopt.nearby.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class UtcTimeConfigTest {

    @Test
    void createsClockInUtc() {
        Clock clock = new UtcTimeConfig().clock();

        assertEquals(ZoneOffset.UTC, clock.getZone());
    }
}
