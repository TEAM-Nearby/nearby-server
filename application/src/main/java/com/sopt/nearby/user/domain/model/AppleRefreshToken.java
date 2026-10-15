// Apple 계정 연동 해제에 사용할 Refresh Token을 표현하는 도메인 모델
package com.sopt.nearby.user.domain.model;

import java.time.LocalDateTime;

public record AppleRefreshToken(
		Long userId,
		String refreshToken,
		LocalDateTime updatedAt
) {
}
