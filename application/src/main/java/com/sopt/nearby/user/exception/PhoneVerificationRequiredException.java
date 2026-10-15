// 동행 프로필 온보딩 전에 휴대폰 인증이 필요한 경우를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ConflictException;

public class PhoneVerificationRequiredException extends ConflictException {

	public PhoneVerificationRequiredException() {
		super(OnboardingErrorCode.PHONE_VERIFICATION_REQUIRED);
	}
}
