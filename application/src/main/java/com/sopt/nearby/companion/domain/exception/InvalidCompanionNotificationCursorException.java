// 동행 알림 페이지 커서가 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidCompanionNotificationCursorException extends BusinessException {

    public InvalidCompanionNotificationCursorException() {
        super(CompanionErrorCode.INVALID_NOTIFICATION_CURSOR);
    }
}
