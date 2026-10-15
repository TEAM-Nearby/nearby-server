// 장소 캐시 조회 또는 저장에 필요한 장소 값을 전달하는 명령 객체다.
package com.sopt.nearby.place.port.in;

import java.math.BigDecimal;

public record ResolvePlaceCacheCommand(
        String googlePlaceId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String category
) {

    public ResolvePlaceCacheCommand(
            final String googlePlaceId,
            final String name,
            final String address,
            final BigDecimal latitude,
            final BigDecimal longitude
    ) {
        this(googlePlaceId, name, address, latitude, longitude, null);
    }
}
