// 동행 일정 확정 권한이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionScheduleException extends BusinessException {

    public ForbiddenCompanionScheduleException() {
        super(CompanionErrorCode.FORBIDDEN_COMPANION_SCHEDULE);
    }
}