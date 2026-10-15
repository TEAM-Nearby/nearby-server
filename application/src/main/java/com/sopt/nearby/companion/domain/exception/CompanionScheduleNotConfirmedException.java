// 확정된 동행 일정이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionScheduleNotConfirmedException extends ConflictException {

    public CompanionScheduleNotConfirmedException() {
        super(CompanionErrorCode.COMPANION_SCHEDULE_NOT_CONFIRMED);
    }
}
