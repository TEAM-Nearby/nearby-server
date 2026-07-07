// 혼밥 맛집 목록 조회 쿼리 결과를 표현한다.
package com.sopt.nearby.place.domain.model;

import java.math.BigDecimal;

public record SoloDiningPlaceSummary(
        Long placeId,
        String googlePlaceId,
        String name,
        String photoReference,
        SoloDiningPlaceCategory category,
        int distanceMeters,
        BigDecimal rating,
        Integer reviewCount,
        boolean isFavorite,
        BigDecimal latitude,
        BigDecimal longitude,
        PlaceBusinessStatus businessStatus
) {
}
