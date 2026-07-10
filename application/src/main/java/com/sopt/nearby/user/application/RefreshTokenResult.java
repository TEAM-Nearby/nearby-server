// 토큰 재발급 결과를 표현하는 응답 모델
package com.sopt.nearby.user.application;

public record RefreshTokenResult(
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
