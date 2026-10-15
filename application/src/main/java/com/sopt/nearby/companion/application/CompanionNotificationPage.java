// 동행 알림 페이지 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.util.List;

public record CompanionNotificationPage(
        List<CompanionNotificationSummary> requests,
        String nextCursor,
        boolean hasNext
) {
}
