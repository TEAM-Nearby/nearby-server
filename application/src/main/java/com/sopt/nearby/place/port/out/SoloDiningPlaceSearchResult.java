// 외부 장소 검색 결과를 장소 캐시 저장용 값으로 표현한다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;

public record SoloDiningPlaceSearchResult(
        String googlePlaceId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        SoloDiningPlaceCategory category,
        BigDecimal rating,
        Integer reviewCount,
        String photoReference,
        PlaceBusinessStatus businessStatus
) {
}
