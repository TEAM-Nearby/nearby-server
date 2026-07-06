// 동행 알림 읽음 처리 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.MarkCompanionNotificationAsReadResult;
import java.time.LocalDateTime;

public record MarkCompanionNotificationAsReadResponse(
        Long notificationId,
        boolean isRead,
        LocalDateTime readAt
) {

    public static MarkCompanionNotificationAsReadResponse from(
            final MarkCompanionNotificationAsReadResult result
    ) {
        return new MarkCompanionNotificationAsReadResponse(
                result.notificationId(),
                result.isRead(),
                result.readAt()
        );
    }
}