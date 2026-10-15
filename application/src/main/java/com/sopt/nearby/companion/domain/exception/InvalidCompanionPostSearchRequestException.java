// 동행 모집글 목록 조회 조건이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionPostSearchRequestException extends BusinessException {

    public InvalidCompanionPostSearchRequestException() {
        super(CompanionErrorCode.VALIDATION_ERROR);
    }
}
