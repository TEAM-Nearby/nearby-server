// 온보딩 과정에서 발생하는 사용자 기능 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum OnboardingErrorCode implements ErrorCode {

	PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED("인증 문자 발송 횟수를 초과했습니다."),
	PHONE_VERIFICATION_CODE_MISMATCH("인증 번호가 일치하지 않습니다."),
	PHONE_VERIFICATION_NOT_FOUND("인증 요청이 발생하지 않았습니다."),
	PHONE_VERIFICATION_EXPIRED("인증 시간이 만료되었습니다."),
	PHONE_VERIFICATION_REQUIRED("휴대폰 인증이 완료되지 않았습니다."),
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
