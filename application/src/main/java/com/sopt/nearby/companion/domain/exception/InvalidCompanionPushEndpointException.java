// 올바르지 않은 동행 푸시 수신 대상일 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.companion.domain.code.CompanionErrorCode;
import com.sopt.nearby.common.exception.BusinessException;

public class InvalidCompanionPushEndpointException extends BusinessException {

    public InvalidCompanionPushEndpointException() {
        super(CompanionErrorCode.INVALID_PUSH_ENDPOINT);
    }
}
