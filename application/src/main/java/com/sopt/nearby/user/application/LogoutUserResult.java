// 로그아웃 처리 결과를 표현하는 응답 모델
package com.sopt.nearby.user.application;

public record LogoutUserResult(
		boolean loggedOut
) {
}
