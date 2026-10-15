// Apple Refresh Token이 없어 재로그인이 필요한 상태를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ConflictException;

public class AppleReauthenticationRequiredException extends ConflictException {

	public AppleReauthenticationRequiredException() {
		super(AccountWithdrawalErrorCode.APPLE_REAUTHENTICATION_REQUIRED);
	}
}
