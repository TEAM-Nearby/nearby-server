// 동행 알림 저장용 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionNotificationEntity;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionNotificationStoreJpaRepository extends JpaRepository<CompanionNotificationEntity, Long> {

    Optional<CompanionNotificationEntity> findByNotificationTypeAndTargetTypeAndTargetIdAndRecipientUserId(
            CompanionNotificationType notificationType,
            CompanionNotificationTargetType targetType,
            Long targetId,
            Long recipientUserId
    );
}
