// 올바르지 않은 만남 인증 요청일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCheckInRequestException extends BusinessException {

    public InvalidCheckInRequestException() {
        super(CompanionErrorCode.INVALID_CHECK_IN_REQUEST);
    }
}
