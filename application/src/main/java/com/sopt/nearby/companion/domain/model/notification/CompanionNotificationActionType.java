// 동행 알림 목록 카드의 버튼 동작 타입을 정의하는 enum
package com.sopt.nearby.companion.domain.model.notification;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;

public enum CompanionNotificationActionType {
    CONFIRM_SCHEDULE,
    VIEW_REJECTION,
    ACCEPT_REQUEST,
    NONE;

    public static CompanionNotificationActionType from(
            final CompanionNotificationDirection direction,
            final CompanionApplicationStatus status
    ) {
        if (direction == null || status == null) {
            return NONE;
        }

        return switch (direction) {
            case SENT -> switch (status) {
                case ACCEPTED -> CONFIRM_SCHEDULE;
                case REJECTED -> VIEW_REJECTION;
                default -> NONE;
            };
            case RECEIVED -> switch (status) {
                case PENDING -> ACCEPT_REQUEST;
                case ACCEPTED -> CONFIRM_SCHEDULE;
                default -> NONE;
            };
        };
    }
}

