// 액세스 토큰과 리프레시 토큰 발급 결과를 표현하는 모델
package com.sopt.nearby.user.application;

public record IssuedTokens(
		String accessToken,
		String refreshToken,
		String refreshTokenHash,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
