// 혼밥 맛집 즐겨찾기 목록 조회 결과를 표현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import java.util.List;

public record SoloDiningFavoritesResult(
        List<SoloDiningFavoriteSummary> favorites
) {
}
