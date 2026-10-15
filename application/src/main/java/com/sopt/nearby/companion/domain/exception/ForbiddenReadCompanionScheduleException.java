// 동행 일정 조회 권한이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenReadCompanionScheduleException extends BusinessException {

    public ForbiddenReadCompanionScheduleException() {
        super(CompanionErrorCode.FORBIDDEN_READ_COMPANION_SCHEDULE);
    }
}
