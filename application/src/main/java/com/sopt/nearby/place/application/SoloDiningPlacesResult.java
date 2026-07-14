// 혼밥 맛집 목록 조회 결과를 표현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import java.math.BigDecimal;
import java.util.List;

public record SoloDiningPlacesResult(
        List<Place> places
) {

    public record Place(
            Long placeId,
            String googlePlaceId,
            String name,
            String address,
            String photoReference,
            String imageUrl,
            SoloDiningPlaceCategory category,
            int distanceMeters,
            BigDecimal rating,
            Integer reviewCount,
            boolean isFavorite,
            BigDecimal latitude,
            BigDecimal longitude,
            PlaceBusinessStatus businessStatus
    ) {

        public static Place from(final SoloDiningPlaceSummary place, final String imageUrl) {
            return new Place(
                    place.placeId(),
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.photoReference(),
                    imageUrl,
                    place.category(),
                    place.distanceMeters(),
                    place.rating(),
                    place.reviewCount(),
                    place.isFavorite(),
                    place.latitude(),
                    place.longitude(),
                    place.businessStatus()
            );
        }
    }
}
