// 애플 로그인 API 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum AppleLoginSuccessCode implements SuccessCode {

	APPLE_LOGIN_SUCCESS("애플 로그인에 성공했습니다.");

	private final String message;

	AppleLoginSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
