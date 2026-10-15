// 대기 중이 아닌 동행 신청을 처리하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionRequestNotPendingException extends ConflictException {

    public CompanionRequestNotPendingException() {
        super(CompanionErrorCode.COMPANION_REQUEST_NOT_PENDING);
    }
}
