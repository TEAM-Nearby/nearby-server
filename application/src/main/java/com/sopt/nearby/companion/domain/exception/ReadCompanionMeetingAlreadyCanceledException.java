// 취소된 동행 만남 상세를 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;

public class ReadCompanionMeetingAlreadyCanceledException extends ConflictException {

    public ReadCompanionMeetingAlreadyCanceledException() {
        super(ReadMeetingErrorCode.COMPANION_MEETING_ALREADY_CANCELED);
    }

    private enum ReadMeetingErrorCode implements ErrorCode {
        COMPANION_MEETING_ALREADY_CANCELED("취소된 동행입니다.");

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
