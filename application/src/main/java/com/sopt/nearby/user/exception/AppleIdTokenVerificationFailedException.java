// Apple ID 토큰의 기본 OIDC 검증 실패를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleIdTokenVerificationFailedException extends BusinessException {

	public AppleIdTokenVerificationFailedException() {
		super(AppleLoginErrorCode.APPLE_ID_TOKEN_VERIFICATION_FAILED);
	}
}
