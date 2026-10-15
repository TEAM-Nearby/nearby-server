// 토큰 재발급에 사용할 리프레시 토큰을 담는 요청 모델
package com.sopt.nearby.user.application;

public record RefreshTokenCommand(
		String refreshToken
) {
}
