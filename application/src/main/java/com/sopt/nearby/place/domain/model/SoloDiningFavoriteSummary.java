// 혼밥 맛집 즐겨찾기 목록 조회 결과를 표현한다.
package com.sopt.nearby.place.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SoloDiningFavoriteSummary(
        Long favoriteId,
        LocalDateTime createdAt,
        Long placeId,
        String googlePlaceId,
        String name,
        String address,
        String photoReference,
        SoloDiningPlaceCategory category,
        List<SoloDiningPlaceCategory> categories,
        int distanceMeters,
        BigDecimal rating,
        Integer reviewCount,
        boolean isFavorite,
        PlaceBusinessStatus businessStatus
) {
}
