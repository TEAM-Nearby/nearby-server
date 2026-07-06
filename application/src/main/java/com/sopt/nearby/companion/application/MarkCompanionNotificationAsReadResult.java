// 동행 알림 읽음 처리 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import java.time.LocalDateTime;

public record MarkCompanionNotificationAsReadResult(
        Long notificationId,
        boolean isRead,
        LocalDateTime readAt
) {
}