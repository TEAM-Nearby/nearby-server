// 동행 알림 목록 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.util.List;

public record CompanionNotificationsResponse(
        CompanionNotificationDirection direction,
        List<CompanionNotificationResponse> requests
) {

    public static CompanionNotificationsResponse from(
            final CompanionNotificationDirection direction,
            final List<CompanionNotificationSummary> summaries
    ) {
        return new CompanionNotificationsResponse(
                direction,
                summaries.stream()
                        .map(CompanionNotificationResponse::from)
                        .toList()
        );
    }
}

