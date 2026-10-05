// 이미 탈퇴한 회원의 중복 요청을 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ConflictException;

public class UserAlreadyWithdrawnException extends ConflictException {

	public UserAlreadyWithdrawnException() {
		super(AccountWithdrawalErrorCode.USER_ALREADY_WITHDRAWN);
	}
}
