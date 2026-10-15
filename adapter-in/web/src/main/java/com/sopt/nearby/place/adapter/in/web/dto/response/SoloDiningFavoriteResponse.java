// 혼밥 맛집 즐겨찾기 상태 변경 API 응답을 표현한다.
package com.sopt.nearby.place.adapter.in.web.dto.response;

import com.sopt.nearby.place.application.SoloDiningFavoriteResult;

public record SoloDiningFavoriteResponse(
        boolean isFavorite
) {

    public static SoloDiningFavoriteResponse from(final SoloDiningFavoriteResult result) {
        return new SoloDiningFavoriteResponse(result.isFavorite());
    }
}
