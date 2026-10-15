// 올바르지 않은 동행 일정 확정 요청일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionScheduleRequestException extends BusinessException {

    public InvalidCompanionScheduleRequestException() {
        super(CompanionErrorCode.INVALID_SCHEDULE_REQUEST);
    }
}