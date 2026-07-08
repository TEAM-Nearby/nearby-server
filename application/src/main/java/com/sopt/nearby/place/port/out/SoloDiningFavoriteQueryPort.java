// 혼밥 맛집 즐겨찾기 목록 조회용 쿼리 포트를 정의한다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.util.List;

public interface SoloDiningFavoriteQueryPort {

    List<SoloDiningFavoriteSummary> findAllByUserId(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            SoloDiningPlaceCategory category,
            SoloDiningFavoriteSort sort
    );
}
