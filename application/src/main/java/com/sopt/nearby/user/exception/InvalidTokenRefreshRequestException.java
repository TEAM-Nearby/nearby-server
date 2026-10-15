// 올바르지 않은 토큰 재발급 요청을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class InvalidTokenRefreshRequestException extends BusinessException {

	public InvalidTokenRefreshRequestException() {
		super(TokenRefreshErrorCode.INVALID_TOKEN_REFRESH_REQUEST);
	}
}
