// 휴대폰 인증 번호가 저장된 인증 번호와 일치하지 않을 때 발생하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class PhoneVerificationCodeMismatchException extends BusinessException {

	public PhoneVerificationCodeMismatchException() {
		super(OnboardingErrorCode.PHONE_VERIFICATION_CODE_MISMATCH);
	}
}
