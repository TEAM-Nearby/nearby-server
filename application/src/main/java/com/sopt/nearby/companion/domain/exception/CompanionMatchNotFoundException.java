// 동행 매칭을 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;


public class CompanionMatchNotFoundException extends NotFoundException {

    public CompanionMatchNotFoundException() {
        super(CompanionErrorCode.COMPANION_MATCH_NOT_FOUND);
    }
}