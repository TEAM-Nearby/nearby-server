// 혼밥 맛집 목록 조회용 장소 캐시 쿼리 포트를 정의한다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import java.math.BigDecimal;
import java.util.List;

public interface SoloDiningPlaceQueryPort {

    List<SoloDiningPlaceSummary> findAllNearby(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            SoloDiningPlaceCategory category,
            int radiusMeters
    );

    List<SoloDiningPlaceSummary> findAllByPlaceIds(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            List<Long> placeIds
    );
}
