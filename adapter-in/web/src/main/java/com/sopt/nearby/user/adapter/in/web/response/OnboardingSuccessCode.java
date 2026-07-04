// 온보딩 API 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum OnboardingSuccessCode implements SuccessCode {

	PHONE_VERIFICATION_CODE_SENT("인증 문자를 발송에 성공했습니다.");

	private final String message;

	OnboardingSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
