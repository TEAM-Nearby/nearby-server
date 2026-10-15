// 동행 알림 커서 페이지 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompanionNotificationPage;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;

public record CompanionNotificationPageResponse(
        CompanionNotificationDirection direction,
        java.util.List<CompanionNotificationResponse> requests,
        String nextCursor,
        boolean hasNext
) {

    public static CompanionNotificationPageResponse from(
            final CompanionNotificationDirection direction,
            final CompanionNotificationPage page
    ) {
        return new CompanionNotificationPageResponse(
                direction,
                page.requests().stream().map(CompanionNotificationResponse::from).toList(),
                page.nextCursor(),
                page.hasNext()
        );
    }
}
