// 혼밥 맛집 목록 조회용 장소 캐시 쿼리 포트를 정의한다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningMapBounds;
import java.math.BigDecimal;
import java.util.List;

public interface SoloDiningPlaceQueryPort {

    // 거리 오름차순·동일 거리의 장소 ID 내림차순으로 조회하며, maxResults가 null이면 개수를 제한하지 않는다.
    List<SoloDiningPlaceSummary> findAllNearby(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            SoloDiningPlaceCategory category,
            int radiusMeters,
            SoloDiningMapBounds bounds,
            Integer maxResults
    );

    List<SoloDiningPlaceSummary> findAllByPlaceIds(
            Long userId,
            BigDecimal latitude,
            BigDecimal longitude,
            List<Long> placeIds
    );
}
