// 동행 알림 목록의 단일 알림 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.notification;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import java.time.LocalDateTime;

public record CompanionNotificationSummary(
        Long notificationId,
        Long applicationId,
        CompanionApplicationStatus applicationStatus,
        CompanionNotificationHostProfile host,
        String placeName,
        LocalDateTime meetingAt,
        Long matchId,
        CompanionNotificationActionType actionType,
        boolean isRead
) {

    public static CompanionNotificationSummary of(
            final CompanionNotificationDirection direction,
            final Long notificationId,
            final Long applicationId,
            final CompanionApplicationStatus applicationStatus,
            final CompanionNotificationHostProfile host,
            final String placeName,
            final LocalDateTime meetingAt,
            final Long matchId,
            final boolean isRead
    ) {
        return new CompanionNotificationSummary(
                notificationId,
                applicationId,
                applicationStatus,
                host,
                placeName,
                meetingAt,
                matchId,
                CompanionNotificationActionType.from(direction, applicationStatus),
                isRead
        );
    }
}

