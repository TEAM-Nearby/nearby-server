// 올바르지 않은 동행 알림 ID로 요청했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionNotificationIdException extends BusinessException {

    public InvalidCompanionNotificationIdException() {
        super(CompanionErrorCode.INVALID_NOTIFICATION_ID);
    }
}