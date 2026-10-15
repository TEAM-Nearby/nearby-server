// 이미 만료 처리된 리프레시 토큰 상태 충돌을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ConflictException;

public class RefreshTokenAlreadyRevokedException extends ConflictException {

	public RefreshTokenAlreadyRevokedException() {
		super(LogoutErrorCode.REFRESH_TOKEN_ALREADY_REVOKED);
	}
}
