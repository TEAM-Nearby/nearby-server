// 자신의 동행 모집글에 신청하거나 수락하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionRequestSelfException extends BusinessException {

    public ForbiddenCompanionRequestSelfException() {
        super(CompanionErrorCode.FORBIDDEN_COMPANION_REQUEST_SELF);
    }
}
