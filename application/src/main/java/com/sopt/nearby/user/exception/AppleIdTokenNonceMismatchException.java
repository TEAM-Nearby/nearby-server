// Apple ID 토큰의 nonce 불일치를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleIdTokenNonceMismatchException extends BusinessException {

	public AppleIdTokenNonceMismatchException() {
		super(AppleLoginErrorCode.APPLE_ID_TOKEN_NONCE_MISMATCH);
	}
}
