// 휴대폰 인증 요청을 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.NotFoundException;

public class PhoneVerificationNotFoundException extends NotFoundException {

	public PhoneVerificationNotFoundException() {
		super(OnboardingErrorCode.PHONE_VERIFICATION_NOT_FOUND);
	}
}
