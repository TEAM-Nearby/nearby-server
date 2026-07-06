// 동행 알림 목록의 단일 알림 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationActionType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.time.LocalDateTime;

public record CompanionNotificationResponse(
        Long applicationId,
        CompanionApplicationStatus applicationStatus,
        CompanionNotificationHostResponse host,
        String placeName,
        LocalDateTime meetingAt,
        Long matchId,
        CompanionNotificationActionType actionType,
        boolean isRead
) {

    public static CompanionNotificationResponse from(final CompanionNotificationSummary summary) {
        return new CompanionNotificationResponse(
                summary.applicationId(),
                summary.applicationStatus(),
                CompanionNotificationHostResponse.from(summary.host()),
                summary.placeName(),
                summary.meetingAt(),
                summary.matchId(),
                summary.actionType(),
                summary.isRead()
        );
    }
}

