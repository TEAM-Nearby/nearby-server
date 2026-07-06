// 동행 알림을 읽음 처리할 권한이 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionNotificationException extends BusinessException {

    public ForbiddenCompanionNotificationException() {
        super(CompanionErrorCode.FORBIDDEN_COMPANION_NOTIFICATION);
    }
}