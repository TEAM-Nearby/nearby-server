// 회원의 혼밥 장소 즐겨찾기를 표현하는 도메인 모델
package com.sopt.nearby.place.domain.model;

import java.time.LocalDateTime;

public record SoloDiningFavorite(
		Long id,
		Long userId,
		Long placeId,
		LocalDateTime createdAt
) {
}
