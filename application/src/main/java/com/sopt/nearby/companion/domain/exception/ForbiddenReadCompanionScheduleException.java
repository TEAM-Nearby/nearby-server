// 동행 일정 조회 권한이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class ForbiddenReadCompanionScheduleException extends BusinessException {

    public ForbiddenReadCompanionScheduleException() {
        super(ReadScheduleErrorCode.FORBIDDEN_COMPANION_SCHEDULE);
    }

    private enum ReadScheduleErrorCode implements ErrorCode {
        FORBIDDEN_COMPANION_SCHEDULE("동행 일정을 조회할 권한이 없습니다.");

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
