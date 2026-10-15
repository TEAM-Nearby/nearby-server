// 외부 혼밥 장소 검색 조건을 표현한다.
package com.sopt.nearby.place.port.out;

import java.math.BigDecimal;
import java.util.List;

public record SoloDiningPlaceSearchRequest(
        BigDecimal latitude,
        BigDecimal longitude,
        int radiusMeters,
        int maxResultCount,
        List<String> includedTypes
) {
}
