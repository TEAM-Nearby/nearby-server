// 카카오 로그인 API 성공 응답 코드를 정의하는 enum
package com.sopt.nearby.user.adapter.in.web.response;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum KakaoLoginSuccessCode implements SuccessCode {

	KAKAO_LOGIN_SUCCESS("카카오 로그인에 성공했습니다.");

	private final String message;

	KakaoLoginSuccessCode(final String message) {
		this.message = message;
	}

	@Override
	public String message() {
		return message;
	}
}
