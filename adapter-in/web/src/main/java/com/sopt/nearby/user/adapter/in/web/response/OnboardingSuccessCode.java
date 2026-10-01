// 온보딩 API 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum OnboardingSuccessCode implements SuccessCode {

	ONBOARDING_STATUS_FOUND("온보딩 상태 조회에 성공했습니다."),
	COMPANION_PROFILE_SKIP_PROCESSED("동행 프로필 건너뛰기 요청을 처리했습니다."),
	EMERGENCY_CONTACT_SAVED("비상 연락망 저장에 성공했습니다."),
	EMERGENCY_CONTACT_FOUND("비상 연락망 조회에 성공했습니다."),

	PHONE_VERIFICATION_CODE_SENT("인증 문자를 발송에 성공했습니다."),
	PHONE_VERIFICATION_CODE_CONFIRMED("휴대폰 인증에 성공했습니다.");

	private final String message;

	OnboardingSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
