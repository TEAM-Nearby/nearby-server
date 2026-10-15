// 만남 인증 위치가 허용 반경 밖일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class OutOfCheckInRadiusException extends BusinessException {

    public OutOfCheckInRadiusException() {
        super(CompanionErrorCode.OUT_OF_CHECK_IN_RADIUS);
    }
}
