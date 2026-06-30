// 동행 프로필의 핵심 속성을 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

import java.math.BigDecimal;

public record CompanionProfile(
		Long id,
		Long userId,
		String nickname,
		UserGender gender,
		Integer birthYear,
		String profileImageUrl,
		String intro,
		BigDecimal mannerScore,
		int reviewCount,
		CompanionProfileStatus status
) {
}
