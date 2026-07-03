// 카카오 로그인 처리 결과와 Nearby 토큰 응답 값을 담는 모델
package com.sopt.nearby.user.application;

import com.sopt.nearby.user.domain.model.UserOnboardingStatus;

public record KakaoLoginResult(
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn,
		Long userId,
		UserOnboardingStatus onboardingStatus
) {
}
