// 이미 같은 동행 모집글에 신청한 이력이 있을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionRequestAlreadyExistsException extends ConflictException {

    public CompanionRequestAlreadyExistsException() {
        super(CompanionErrorCode.COMPANION_REQUEST_ALREADY_EXISTS);
    }
}
