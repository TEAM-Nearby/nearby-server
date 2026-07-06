// 동행 알림 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.notification;

import java.time.LocalDateTime;

public record CompanionNotification(
        Long id,
        Long recipientUserId,
        CompanionNotificationType notificationType,
        CompanionNotificationTargetType targetType,
        Long targetId,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
}
