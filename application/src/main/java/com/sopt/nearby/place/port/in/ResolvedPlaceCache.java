// 조회되거나 저장된 장소 캐시의 스냅샷을 반환하는 결과 객체다.
package com.sopt.nearby.place.port.in;

import java.math.BigDecimal;

public record ResolvedPlaceCache(
        Long placeId,
        String googlePlaceId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String category
) {

    public ResolvedPlaceCache(final Long placeId) {
        this(placeId, null, null, null, null, null, null);
    }
}
