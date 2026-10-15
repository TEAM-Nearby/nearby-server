// 애플 로그인 실패를 표현하는 에러 코드를 정의하는 enum
package com.sopt.nearby.user.exception;

import com.sopt.nearby.common.exception.ErrorCode;

public enum AppleLoginErrorCode implements ErrorCode {
    APPLE_LOGIN_FAILED("애플 로그인 처리에 실패했습니다."),
    APPLE_CLIENT_ID_NOT_CONFIGURED("서버에 Apple Client ID가 설정되지 않았습니다."),
    APPLE_ID_TOKEN_VERIFICATION_FAILED("애플 ID 토큰의 서명, 만료 시간 또는 발급자 검증에 실패했습니다."),
    APPLE_ID_TOKEN_AUDIENCE_MISMATCH("애플 ID 토큰의 audience가 서버의 Client ID와 일치하지 않습니다."),
    APPLE_ID_TOKEN_NONCE_MISMATCH("애플 ID 토큰의 nonce가 로그인 요청과 일치하지 않습니다."),
    APPLE_ID_TOKEN_SUBJECT_MISSING("애플 ID 토큰에 사용자 식별자(sub)가 없습니다.");

    private final String message;

    AppleLoginErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
