// 동행 신청을 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionRequestNotFoundException extends NotFoundException {

    public CompanionRequestNotFoundException() {
        super(CompanionErrorCode.COMPANION_REQUEST_NOT_FOUND);
    }
}
