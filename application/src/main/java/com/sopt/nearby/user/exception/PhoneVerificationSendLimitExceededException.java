// 휴대폰 인증 문자 발송 한도 초과를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class PhoneVerificationSendLimitExceededException extends BusinessException {

	public PhoneVerificationSendLimitExceededException() {
		super(OnboardingErrorCode.PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED);
	}
}
