// 동행 알림 생성을 요청하는 command 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;

public record CreateCompanionNotificationCommand(
        Long recipientUserId,
        CompanionNotificationType notificationType,
        CompanionNotificationTargetType targetType,
        Long targetId
) {
}
