// 올바르지 않은 동행 만남 ID일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class InvalidCompanionMeetingIdException extends BusinessException {

    public InvalidCompanionMeetingIdException() {
        super(ReadMeetingErrorCode.INVALID_MEETING_ID);
    }

    private enum ReadMeetingErrorCode implements ErrorCode {
        INVALID_MEETING_ID("올바르지 않은 만남 ID입니다.");

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
