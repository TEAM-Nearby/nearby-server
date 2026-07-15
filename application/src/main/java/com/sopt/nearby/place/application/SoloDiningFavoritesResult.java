// 혼밥 맛집 즐겨찾기 목록 조회 결과를 표현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SoloDiningFavoritesResult(
        List<Favorite> favorites
) {

    public record Favorite(
            Long favoriteId,
            LocalDateTime createdAt,
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
            PlaceBusinessStatus businessStatus
    ) {

        public static Favorite from(final SoloDiningFavoriteSummary favorite, final String imageUrl) {
            return new Favorite(
                    favorite.favoriteId(),
                    favorite.createdAt(),
                    favorite.placeId(),
                    favorite.googlePlaceId(),
                    favorite.name(),
                    favorite.address(),
                    favorite.photoReference(),
                    imageUrl,
                    favorite.category(),
                    favorite.distanceMeters(),
                    favorite.rating(),
                    favorite.reviewCount(),
                    favorite.isFavorite(),
                    favorite.businessStatus()
            );
        }
    }
}
