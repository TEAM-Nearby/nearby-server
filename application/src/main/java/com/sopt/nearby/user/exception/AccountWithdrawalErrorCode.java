// 회원 탈퇴 과정에서 발생하는 오류 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum AccountWithdrawalErrorCode implements ErrorCode {
	USER_ACCOUNT_NOT_FOUND("회원 정보를 찾을 수 없습니다."),
	USER_ALREADY_WITHDRAWN("이미 탈퇴한 회원입니다."),
	APPLE_REAUTHENTICATION_REQUIRED("Apple 계정 연동 해제를 위해 Apple 로그인을 다시 진행해 주세요."),
	SOCIAL_ACCOUNT_UNLINK_FAILED("소셜 계정 연동 해제에 실패했습니다.");

	private final String message;

	AccountWithdrawalErrorCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
