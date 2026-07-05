// 완료된 동행 매칭의 일정을 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;

public class CompletedCompanionScheduleNotReadableException extends ConflictException {

    public CompletedCompanionScheduleNotReadableException() {
        super(ReadScheduleErrorCode.COMPANION_MATCH_ALREADY_COMPLETED);
    }

    private enum ReadScheduleErrorCode implements ErrorCode {
        COMPANION_MATCH_ALREADY_COMPLETED("완료된 매칭의 일정은 조회할 수 없습니다.");

        private final String message;

        ReadScheduleErrorCode(final String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}