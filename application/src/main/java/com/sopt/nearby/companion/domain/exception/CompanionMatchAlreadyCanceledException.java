// 취소된 동행 매칭의 일정을 확정하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionMatchAlreadyCanceledException extends ConflictException {

    public CompanionMatchAlreadyCanceledException() {
        super(CompanionErrorCode.COMPANION_MATCH_ALREADY_CANCELED);
    }
}