// 애플 로그인 실패를 표현하는 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum AppleLoginErrorCode implements ErrorCode {
    APPLE_LOGIN_FAILED("애플 ID 토큰이 유효하지 않거나 OIDC 검증에 실패했습니다");

    private final String message;

    AppleLoginErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
