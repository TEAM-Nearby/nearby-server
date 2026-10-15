// 카카오 로그인과 OIDC 검증 실패를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class KakaoLoginFailedException extends BusinessException {

	public KakaoLoginFailedException() {
		super(KakaoLoginErrorCode.KAKAO_LOGIN_FAILED);
	}
}
