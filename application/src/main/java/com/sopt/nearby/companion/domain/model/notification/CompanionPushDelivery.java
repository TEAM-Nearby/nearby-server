// 동행 알림을 특정 기기로 전달하는 발송 작업을 표현한다.
package com.sopt.nearby.companion.domain.model.notification;

import java.time.LocalDateTime;

public record CompanionPushDelivery(
        Long id,
        Long notificationId,
        Long endpointId,
        Long recipientUserId,
        String token,
        String title,
        String body,
        CompanionNotificationTargetType targetType,
        Long targetId,
        CompanionPushDeliveryStatus status,
        int attemptCount,
        LocalDateTime nextAttemptAt,
        LocalDateTime leaseUntil,
        String claimToken,
        String providerMessageId,
        String lastErrorCode,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static CompanionPushDelivery pending(
            final CompanionNotification notification,
            final CompanionPushEndpoint endpoint,
            final String title,
            final String body,
            final LocalDateTime now,
            final LocalDateTime expiresAt
    ) {
        return new CompanionPushDelivery(
                null,
                notification.id(),
                endpoint.id(),
                notification.recipientUserId(),
                endpoint.token(),
                title,
                body,
                notification.targetType(),
                notification.targetId(),
                CompanionPushDeliveryStatus.PENDING,
                0,
                now,
                null,
                null,
                null,
                null,
                expiresAt,
                now,
                now
        );
    }
}
