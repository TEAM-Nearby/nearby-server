// 회원의 리프레시 토큰 저장 정보를 표현하는 도메인 모델
package com.sopt.nearby.user.domain.model;

import java.time.LocalDateTime;

public record RefreshToken(
		Long id,
		Long userId,
		String tokenHash,
		LocalDateTime expiresAt,
		LocalDateTime revokedAt
) {
}
