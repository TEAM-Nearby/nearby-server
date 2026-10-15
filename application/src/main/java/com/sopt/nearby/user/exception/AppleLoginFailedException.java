// 애플 로그인과 OIDC 검증 실패를 표현하는 예외
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.BusinessException;

public class AppleLoginFailedException extends BusinessException {

    public AppleLoginFailedException() {
        super(AppleLoginErrorCode.APPLE_LOGIN_FAILED);
    }
}