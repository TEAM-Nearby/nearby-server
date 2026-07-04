// 휴대폰 인증 번호 확인 결과를 표현하는 응답 모델
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserOnboardingStatus;

public record ConfirmPhoneVerificationCodeResult(
		boolean phoneVerified,
		UserOnboardingStatus onboardingStatus
) {
}
