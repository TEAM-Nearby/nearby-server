// 동행 알림 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CompanionNotificationRepository extends DomainRepository<CompanionNotification, Long> {

    // 존재하는 알림만 한 번에 조회하며 반환 순서는 보장하지 않는다.
    List<CompanionNotification> findAllByIds(List<Long> notificationIds);

    Optional<CompanionNotification> findByUniqueKey(
            CompanionNotificationType notificationType,
            CompanionNotificationTargetType targetType,
            Long targetId,
            Long recipientUserId
    );

    boolean markAsReadIfUnread(Long notificationId, Long recipientUserId, LocalDateTime readAt);
}
