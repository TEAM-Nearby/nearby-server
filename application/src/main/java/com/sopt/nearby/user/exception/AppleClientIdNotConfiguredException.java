// Apple Client ID 설정 누락을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleClientIdNotConfiguredException extends BusinessException {

	public AppleClientIdNotConfiguredException() {
		super(AppleLoginErrorCode.APPLE_CLIENT_ID_NOT_CONFIGURED);
	}
}
