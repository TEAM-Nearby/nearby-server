// 혼밥 맛집 목록 조회 결과를 표현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import java.util.List;

public record SoloDiningPlacesResult(
        List<SoloDiningPlaceSummary> places
) {
}
