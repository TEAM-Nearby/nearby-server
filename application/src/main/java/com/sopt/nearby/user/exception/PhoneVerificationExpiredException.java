// 휴대폰 인증 요청의 유효 시간이 만료되었을 때 발생하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class PhoneVerificationExpiredException extends BusinessException {

	public PhoneVerificationExpiredException() {
		super(OnboardingErrorCode.PHONE_VERIFICATION_EXPIRED);
	}
}
