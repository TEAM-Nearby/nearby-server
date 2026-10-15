// 인증 API의 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum AuthSuccessCode implements SuccessCode {

	LOGOUT_USER("로그아웃되었어요."),
	REFRESH_TOKEN("토큰을 재발급했어요.");

	private final String message;

	AuthSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
