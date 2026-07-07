// 로그아웃 요청에 필요한 사용자와 리프레시 토큰 값을 담는 커맨드
package com.sopt.nearby.user.application;

public record LogoutUserCommand(
		Long userId,
		String refreshToken
) {
}
