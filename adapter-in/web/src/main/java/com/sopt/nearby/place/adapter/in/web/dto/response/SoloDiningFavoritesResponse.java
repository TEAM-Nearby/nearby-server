// 혼밥 맛집 즐겨찾기 목록 조회 API 응답을 표현한다.
package com.sopt.nearby.place.adapter.in.web.dto.response;

import com.sopt.nearby.place.application.SoloDiningFavoritesResult;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SoloDiningFavoritesResponse(
        int totalCount,
        List<FavoriteResponse> favorites
) {

    public static SoloDiningFavoritesResponse from(final SoloDiningFavoritesResult result) {
        List<FavoriteResponse> favorites = result.favorites().stream()
                .map(FavoriteResponse::from)
                .toList();
        return new SoloDiningFavoritesResponse(favorites.size(), favorites);
    }

    public record FavoriteResponse(
            Long favoriteId,
            LocalDateTime createdAt,
            Long placeId,
            String googlePlaceId,
            String name,
            String photoReference,
            String category,
            int distanceMeters,
            BigDecimal rating,
            Integer reviewCount,
            boolean isFavorite,
            String businessStatus
    ) {

        static FavoriteResponse from(final SoloDiningFavoriteSummary favorite) {
            return new FavoriteResponse(
                    favorite.favoriteId(),
                    favorite.createdAt(),
                    favorite.placeId(),
                    favorite.googlePlaceId(),
                    favorite.name(),
                    favorite.photoReference(),
                    favorite.category() == null ? null : favorite.category().name(),
                    favorite.distanceMeters(),
                    favorite.rating(),
                    favorite.reviewCount(),
                    favorite.isFavorite(),
                    favorite.businessStatus().name()
            );
        }
    }
}
