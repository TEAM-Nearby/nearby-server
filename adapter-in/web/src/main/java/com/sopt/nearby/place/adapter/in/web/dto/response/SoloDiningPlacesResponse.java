// 혼밥 맛집 목록 조회 API 응답을 표현한다.
package com.sopt.nearby.place.adapter.in.web.dto.response;

import com.sopt.nearby.place.application.SoloDiningPlacesResult;
import java.math.BigDecimal;
import java.util.List;

public record SoloDiningPlacesResponse(
        List<PlaceResponse> places
) {

    public static SoloDiningPlacesResponse from(final SoloDiningPlacesResult result) {
        return new SoloDiningPlacesResponse(result.places().stream()
                .map(PlaceResponse::from)
                .toList());
    }

    public record PlaceResponse(
            Long placeId,
            String googlePlaceId,
            String name,
            String address,
            String photoReference,
            String imageUrl,
            String category,
            int distanceMeters,
            BigDecimal rating,
            Integer reviewCount,
            boolean isFavorite,
            BigDecimal latitude,
            BigDecimal longitude,
            String businessStatus
    ) {

        static PlaceResponse from(final SoloDiningPlacesResult.Place place) {
            return new PlaceResponse(
                    place.placeId(),
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.photoReference(),
                    place.imageUrl(),
                    place.category().name(),
                    place.distanceMeters(),
                    place.rating(),
                    place.reviewCount(),
                    place.isFavorite(),
                    place.latitude(),
                    place.longitude(),
                    place.businessStatus().name()
            );
        }
    }
}
