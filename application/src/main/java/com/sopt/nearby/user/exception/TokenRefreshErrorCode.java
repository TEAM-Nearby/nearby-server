// 토큰 재발급 요청 검증 오류 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum TokenRefreshErrorCode implements ErrorCode {

	INVALID_TOKEN_REFRESH_REQUEST("올바르지 않은 토큰 재발급 요청입니다.");

	private final String message;

	TokenRefreshErrorCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
