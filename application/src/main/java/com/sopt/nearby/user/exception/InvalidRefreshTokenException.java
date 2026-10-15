// 유효하지 않은 리프레시 토큰을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class InvalidRefreshTokenException extends BusinessException {

	public InvalidRefreshTokenException() {
		super(LogoutErrorCode.INVALID_REFRESH_TOKEN);
	}
}
