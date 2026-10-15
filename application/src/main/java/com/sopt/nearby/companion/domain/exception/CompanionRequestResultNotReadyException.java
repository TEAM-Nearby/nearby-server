// 아직 확정되지 않은 동행 신청 결과를 조회할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionRequestResultNotReadyException extends ConflictException {

    public CompanionRequestResultNotReadyException() {
        super(CompanionErrorCode.COMPANION_REQUEST_RESULT_NOT_READY);
    }
}
