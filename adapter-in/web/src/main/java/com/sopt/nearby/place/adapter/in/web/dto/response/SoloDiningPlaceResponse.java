// 혼밥 맛집 상세 조회 API 응답을 표현한다.
package com.sopt.nearby.place.adapter.in.web.dto.response;

import com.sopt.nearby.place.application.SoloDiningPlaceResult;
import java.math.BigDecimal;
import java.util.List;

public record SoloDiningPlaceResponse(
        Long placeId,
        String googlePlaceId,
        String name,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        int distanceMeters,
        BigDecimal rating,
        Integer reviewCount,
        String phoneNumber,
        String photoReference,
        List<String> photoReferences,
        String imageUrl,
        String businessStatus,
        String priceLevel,
        String priceRange,
        List<String> regularOpeningHours,
        String editorialSummary,
        boolean isFavorite
) {

    public static SoloDiningPlaceResponse from(final SoloDiningPlaceResult result) {
        return new SoloDiningPlaceResponse(
                result.placeId(),
                result.googlePlaceId(),
                result.name(),
                result.address(),
                result.latitude(),
                result.longitude(),
                result.category() == null ? null : result.category().name(),
                result.distanceMeters(),
                result.rating(),
                result.reviewCount(),
                result.phoneNumber(),
                result.photoReference(),
                result.photoReferences(),
                result.imageUrl(),
                result.businessStatus().name(),
                result.priceLevel(),
                result.priceRange(),
                result.regularOpeningHours(),
                result.editorialSummary(),
                result.isFavorite()
        );
    }
}
