// 동행 알림 목록의 단일 알림 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.notification;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CompanionNotificationSummary(
        Long notificationId,
        Long applicationId,
        CompanionApplicationStatus applicationStatus,
        CompanionNotificationHostProfile host,
        String placeName,
        LocalDateTime meetingAt,
        Long matchId,
        CompanionNotificationActionType actionType,
        boolean isRead,
        String placeAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {

    public CompanionNotificationSummary(Long notificationId, Long applicationId,
            CompanionApplicationStatus applicationStatus, CompanionNotificationHostProfile host,
            String placeName, LocalDateTime meetingAt, Long matchId,
            CompanionNotificationActionType actionType, boolean isRead) {
        this(notificationId, applicationId, applicationStatus, host, placeName, meetingAt, matchId,
                actionType, isRead, null);
    }

    public CompanionNotificationSummary(Long notificationId, Long applicationId,
            CompanionApplicationStatus applicationStatus, CompanionNotificationHostProfile host,
            String placeName, LocalDateTime meetingAt, Long matchId,
            CompanionNotificationActionType actionType, boolean isRead, String placeAddress) {
        this(notificationId, applicationId, applicationStatus, host, placeName, meetingAt, matchId,
                actionType, isRead, placeAddress, null, null);
    }

    public static CompanionNotificationSummary of(
            final CompanionNotificationDirection direction,
            final CompanionNotificationType notificationType,
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
                CompanionNotificationActionType.from(direction, applicationStatus, notificationType),
                isRead
        );
    }

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
        return of(
                direction,
                null,
                notificationId,
                applicationId,
                applicationStatus,
                host,
                placeName,
                meetingAt,
                matchId,
                isRead
        );
    }
}

