// 로그아웃 과정에서 발생하는 사용자 기능 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum LogoutErrorCode implements ErrorCode {

	INVALID_LOGOUT_REQUEST("올바르지 않은 로그아웃 요청입니다."),
	INVALID_REFRESH_TOKEN("유효하지 않은 refreshToken입니다."),
	REFRESH_TOKEN_ALREADY_REVOKED("이미 만료 처리된 refreshToken입니다.");

	private final String message;

	LogoutErrorCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
