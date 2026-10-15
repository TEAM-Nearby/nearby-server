// 올바르지 않은 동행 매칭 ID일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionMatchIdException extends BusinessException {

    public InvalidCompanionMatchIdException() {
        super(CompanionErrorCode.INVALID_MATCH_ID);
    }
}
