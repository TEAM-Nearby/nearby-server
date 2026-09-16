// 동행 알림 페이지 조회 행과 정렬 위치를 표현한다.
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.time.LocalDateTime;

public record CompanionNotificationPageRow(
        Long notificationId,
        LocalDateTime createdAt,
        CompanionNotificationSummary summary
) {
}
