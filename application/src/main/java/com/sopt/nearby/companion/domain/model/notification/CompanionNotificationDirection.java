// 동행 알림 목록의 조회 방향을 정의하는 enum
package com.sopt.nearby.companion.domain.model.notification;

import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationDirectionException;

public enum CompanionNotificationDirection {
    SENT,
    RECEIVED;

    public static CompanionNotificationDirection from(final String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidCompanionNotificationDirectionException();
        }

        try {
            return CompanionNotificationDirection.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            throw new InvalidCompanionNotificationDirectionException();
        }
    }
}

