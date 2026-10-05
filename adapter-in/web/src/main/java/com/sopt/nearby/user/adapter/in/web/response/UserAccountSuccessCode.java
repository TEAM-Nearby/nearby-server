// 회원 계정 API의 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum UserAccountSuccessCode implements SuccessCode {
	WITHDRAW_USER("회원 탈퇴가 완료되었어요.");

	private final String message;

	UserAccountSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
