// 혼밥 맛집 상세 조회 조건을 애플리케이션 계층으로 전달한다.
package com.sopt.nearby.place.application;

import java.math.BigDecimal;

public record ReadSoloDiningPlaceCommand(
        Long userId,
        Long placeId,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
