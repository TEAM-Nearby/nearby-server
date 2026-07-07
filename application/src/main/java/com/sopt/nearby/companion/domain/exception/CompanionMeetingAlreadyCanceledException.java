// 취소된 동행 만남에 인증하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionMeetingAlreadyCanceledException extends ConflictException {

    public CompanionMeetingAlreadyCanceledException() {
        super(CompanionErrorCode.COMPANION_MEETING_ALREADY_CANCELED);
    }
}
