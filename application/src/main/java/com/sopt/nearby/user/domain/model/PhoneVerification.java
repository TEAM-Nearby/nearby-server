// 휴대폰 인증 요청 정보를 표현하는 도메인 모델
package com.sopt.nearby.user.domain.model;

import java.time.LocalDateTime;

public record PhoneVerification(
		Long id,
		Long userId,
		String phoneNumber,
		String carrier,
		PhoneVerificationStatus status,
		LocalDateTime expiresAt,
		LocalDateTime verifiedAt
) {
}
