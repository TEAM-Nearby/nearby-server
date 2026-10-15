// 이미 확정된 동행 일정이 있을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionScheduleAlreadyConfirmedException extends ConflictException {

    public CompanionScheduleAlreadyConfirmedException() {
        super(CompanionErrorCode.COMPANION_SCHEDULE_ALREADY_CONFIRMED);
    }
}