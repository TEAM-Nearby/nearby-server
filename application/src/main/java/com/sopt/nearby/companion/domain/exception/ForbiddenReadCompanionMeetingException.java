// 동행 만남 상세 조회 권한이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class ForbiddenReadCompanionMeetingException extends BusinessException {

    public ForbiddenReadCompanionMeetingException() {
        super(ReadMeetingErrorCode.FORBIDDEN_COMPANION_MEETING);
    }

    private enum ReadMeetingErrorCode implements ErrorCode {
        FORBIDDEN_COMPANION_MEETING("해당 동행 정보를 조회할 권한이 없습니다.");

        private final String message;

        ReadMeetingErrorCode(final String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
