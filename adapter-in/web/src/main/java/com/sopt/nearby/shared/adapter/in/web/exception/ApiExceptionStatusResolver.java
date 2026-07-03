package com.sopt.nearby.shared.adapter.in.web.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;
import com.sopt.nearby.common.exception.NotFoundException;
import org.springframework.http.HttpStatus;

public final class ApiExceptionStatusResolver {

    private static final String FORBIDDEN_PREFIX = "FORBIDDEN";
    private static final String UNAUTHORIZED_CODE = "UNAUTHORIZED";
    private static final String KAKAO_LOGIN_FAILED_CODE = "KAKAO_LOGIN_FAILED";

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
        if (errorCode.name().startsWith(FORBIDDEN_PREFIX)) {
            return HttpStatus.FORBIDDEN;
        }
        if (errorCode.name().equals(UNAUTHORIZED_CODE) || errorCode.name().equals(KAKAO_LOGIN_FAILED_CODE)) {
            return HttpStatus.UNAUTHORIZED;
        }

        return HttpStatus.BAD_REQUEST;
    }
}