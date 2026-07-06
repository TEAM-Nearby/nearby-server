// 동행 알림을 찾지 못했을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionNotificationNotFoundException extends NotFoundException {

    public CompanionNotificationNotFoundException() {
        super(CompanionErrorCode.COMPANION_NOTIFICATION_NOT_FOUND);
    }
}