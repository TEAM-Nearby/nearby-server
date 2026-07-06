// 예외 유형과 ErrorCode를 기반으로 HTTP 상태 코드를 결정하는 유틸리티
package com.sopt.nearby.shared.adapter.in.web.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;
import com.sopt.nearby.common.exception.NotFoundException;
import org.springframework.http.HttpStatus;

public final class ApiExceptionStatusResolver {

    private static final String FORBIDDEN_PREFIX = "FORBIDDEN";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String KAKAO_LOGIN_FAILED_CODE = "KAKAO_LOGIN_FAILED";
    private static final String ONBOARDING_REQUIRED_CODE = "ONBOARDING_REQUIRED";
    private static final String COMPANION_POST_EXPIRED_CODE = "COMPANION_POST_EXPIRED";
    private static final String PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED_CODE =
            "PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED";
    private static final String PHONE_VERIFICATION_EXPIRED_CODE = "PHONE_VERIFICATION_EXPIRED";

    private ApiExceptionStatusResolver() {
    }

    public static HttpStatus resolve(final BusinessException exception) {
        return resolve(exception.getClass(), exception.getErrorCode());
    }

    public static HttpStatus resolve(
            final Class<? extends BusinessException> exceptionClass,
            final ErrorCode errorCode
    ) {
        if (NotFoundException.class.isAssignableFrom(exceptionClass)) {
            return HttpStatus.NOT_FOUND;
        }
        if (ConflictException.class.isAssignableFrom(exceptionClass)) {
            return HttpStatus.CONFLICT;
        }
        if (errorCode.name().startsWith(FORBIDDEN_PREFIX)) {
            return HttpStatus.FORBIDDEN;
        }
        if (errorCode.name().equals(ONBOARDING_REQUIRED_CODE)) {
            return HttpStatus.FORBIDDEN;
        }
        if (errorCode.name().equals(UNAUTHORIZED_CODE) || errorCode.name().equals(KAKAO_LOGIN_FAILED_CODE)) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (errorCode.name().equals(PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED_CODE)) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (errorCode.name().equals(PHONE_VERIFICATION_EXPIRED_CODE)) {
            return HttpStatus.GONE;
        }
        if (errorCode.name().equals(COMPANION_POST_EXPIRED_CODE)) {
            return HttpStatus.GONE;
        }

        return HttpStatus.BAD_REQUEST;
    }
}
