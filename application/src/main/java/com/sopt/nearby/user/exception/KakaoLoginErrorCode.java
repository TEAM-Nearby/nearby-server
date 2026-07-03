// 카카오 로그인 실패를 표현하는 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum KakaoLoginErrorCode implements ErrorCode {

	KAKAO_LOGIN_FAILED("카카오 ID 토큰이 유효하지 않거나 OIDC 검증에 실패했습니다.");

	private final String message;

	KakaoLoginErrorCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
