// 올바르지 않은 동행 알림 조회 방향일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionNotificationDirectionException extends BusinessException {

    public InvalidCompanionNotificationDirectionException() {
        super(CompanionErrorCode.INVALID_REQUEST_DIRECTION);
    }
}

