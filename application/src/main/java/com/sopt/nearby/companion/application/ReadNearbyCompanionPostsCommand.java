// 주변 동행 모집글 목록 조회 조건을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import java.math.BigDecimal;

public record ReadNearbyCompanionPostsCommand(
        Long userId,
        BigDecimal latitude,
        BigDecimal longitude,
        int radiusMeters,
        CompanionPostPlaceCategory placeCategory,
        CompanionPostSort sort
) {
}
