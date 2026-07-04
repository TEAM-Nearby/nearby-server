// 온보딩 과정에서 발생하는 사용자 기능 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum OnboardingErrorCode implements ErrorCode {

	PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED("인증 문자 발송 횟수를 초과했습니다."),
	USER_NOT_FOUND("사용자를 찾을 수 없습니다.");

	private final String message;

	OnboardingErrorCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
