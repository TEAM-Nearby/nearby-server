// 장소 좌표에 해당하는 IANA 시간대를 조회하는 출력 포트다.
package com.sopt.nearby.companion.port.out;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

public interface CompanionPlaceTimeZoneLookup {

    Optional<ZoneId> findTimeZone(BigDecimal latitude, BigDecimal longitude, Instant instant);

    default Optional<ZoneId> findTimeZone(
            final BigDecimal latitude,
            final BigDecimal longitude,
            final Instant instant,
            final Duration timeout
    ) {
        return findTimeZone(latitude, longitude, instant);
    }
}
