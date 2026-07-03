// Nearby 자체 토큰 발급에 필요한 회원 인증 클레임을 담는 요청 모델
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.domain.model.UserRole;

public record TokenIssueRequest(
		Long userId,
		UserRole role,
		UserOnboardingStatus onboardingStatus
) {
}
