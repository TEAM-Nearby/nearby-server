// 한국 표준시 Clock 설정을 검증한다.
package com.sopt.nearby.shared.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class KoreaTimeConfigTest {

    @Test
    void createsClockInKoreaStandardTime() {
        Clock clock = new KoreaTimeConfig().koreaStandardClock();

        assertEquals(ZoneId.of("Asia/Seoul"), clock.getZone());
    }
}
