// 회원 계정의 핵심 속성을 표현하는 도메인 모델
package com.sopt.nearby.domain.user.model;

import java.time.LocalDateTime;

public record UserAccount(
		Long id,
		UserRole role,
		UserAccountStatus status,
		String phoneNumber,
		LocalDateTime phoneVerifiedAt,
		UserOnboardingStatus onboardingStatus,
		LocalDateTime createdAt,
		LocalDateTime deletedAt
) {
}
