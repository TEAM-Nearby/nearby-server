// 혼밥 맛집 즐겨찾기 목록 조회 조건을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;

public record ReadSoloDiningFavoritesCommand(
        Long userId,
        BigDecimal latitude,
        BigDecimal longitude,
        SoloDiningPlaceCategory category,
        SoloDiningFavoriteSort sort
) {
}
