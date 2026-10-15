// 올바르지 않은 로그아웃 요청을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class InvalidLogoutRequestException extends BusinessException {

	public InvalidLogoutRequestException() {
		super(LogoutErrorCode.INVALID_LOGOUT_REQUEST);
	}
}
