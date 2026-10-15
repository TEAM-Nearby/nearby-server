// Apple ID 토큰의 audience 불일치를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleIdTokenAudienceMismatchException extends BusinessException {

	public AppleIdTokenAudienceMismatchException() {
		super(AppleLoginErrorCode.APPLE_ID_TOKEN_AUDIENCE_MISMATCH);
	}
}
