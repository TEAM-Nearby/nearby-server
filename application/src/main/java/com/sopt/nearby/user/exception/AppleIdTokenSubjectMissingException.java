// Apple ID 토큰의 사용자 식별자 누락을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleIdTokenSubjectMissingException extends BusinessException {

	public AppleIdTokenSubjectMissingException() {
		super(AppleLoginErrorCode.APPLE_ID_TOKEN_SUBJECT_MISSING);
	}
}
