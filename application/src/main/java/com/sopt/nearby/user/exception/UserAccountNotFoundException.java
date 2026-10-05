// 탈퇴할 회원 계정을 찾지 못한 경우를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.NotFoundException;

public class UserAccountNotFoundException extends NotFoundException {

	public UserAccountNotFoundException() {
		super(AccountWithdrawalErrorCode.USER_ACCOUNT_NOT_FOUND);
	}
}
