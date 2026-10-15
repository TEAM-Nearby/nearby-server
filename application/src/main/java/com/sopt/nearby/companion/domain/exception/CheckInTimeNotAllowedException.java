// 만남 인증 가능 시간이 아닐 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CheckInTimeNotAllowedException extends ConflictException {

    public CheckInTimeNotAllowedException() {
        super(CompanionErrorCode.CHECK_IN_TIME_NOT_ALLOWED);
    }
}
