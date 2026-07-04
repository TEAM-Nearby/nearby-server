package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionMatchException extends BusinessException {
    public ForbiddenCompanionMatchException() {
        super(CompanionErrorCode.FORBIDDEN_COMPANION_MATCH);
    }
}
