// 취소된 동행 매칭의 일정을 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionMatchScheduleNotReadableException extends ConflictException {

    public CompanionMatchScheduleNotReadableException() {
        super(CompanionErrorCode.COMPANION_MATCH_SCHEDULE_NOT_READABLE);
    }
}