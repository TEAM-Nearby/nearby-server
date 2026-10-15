// 동행 만남 정보를 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionMeetingNotFoundException extends NotFoundException {

    public CompanionMeetingNotFoundException() {
        super(CompanionErrorCode.COMPANION_MEETING_NOT_FOUND);
    }
}
