// 혼밥 맛집 상세 조회 결과를 표현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.util.List;

public record SoloDiningPlaceResult(
        Long placeId,
        String googlePlaceId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        SoloDiningPlaceCategory category,
        int distanceMeters,
        BigDecimal rating,
        Integer reviewCount,
        String phoneNumber,
        String photoReference,
        List<String> photoReferences,
        PlaceBusinessStatus businessStatus,
        String priceLevel,
        String priceRange,
        List<String> regularOpeningHours,
        String editorialSummary,
        boolean isFavorite
) {
}
