// Nearby 자체 토큰 발급 결과를 표현하는 모델
package com.sopt.nearby.user.application;

public record IssuedNearbyTokens(
		String accessToken,
		String refreshToken,
		String refreshTokenHash,
		long accessTokenExpiresIn,
		long refreshTokenExpiresIn
) {
}
