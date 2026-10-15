// 동행 알림 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionNotificationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationStoreJpaRepository;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CompanionNotificationRepositoryAdapter
        extends SimpleJpaRepositoryAdapter<CompanionNotification, Long, CompanionNotificationEntity, Long>
        implements CompanionNotificationRepository {

    private final CompanionNotificationStoreJpaRepository jpaRepository;

    public CompanionNotificationRepositoryAdapter(final CompanionNotificationStoreJpaRepository jpaRepository) {
        super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
                Function.identity());
        this.jpaRepository = jpaRepository;
    }

    @Override
    public CompanionNotification save(final CompanionNotification model) {
        try {
            return CompanionPersistenceMapper.toDomain(
                    jpaRepository.saveAndFlush(CompanionPersistenceMapper.toEntity(model))
            );
        } catch (DataIntegrityViolationException exception) {
            return findByUniqueKey(
                    model.notificationType(),
                    model.targetType(),
                    model.targetId(),
                    model.recipientUserId()
            ).orElseThrow(() -> exception);
        }
    }

    @Override
    public Optional<CompanionNotification> findByUniqueKey(
            final CompanionNotificationType notificationType,
            final CompanionNotificationTargetType targetType,
            final Long targetId,
            final Long recipientUserId
    ) {
        return jpaRepository.findByNotificationTypeAndTargetTypeAndTargetIdAndRecipientUserId(
                notificationType,
                targetType,
                targetId,
                recipientUserId
        ).map(CompanionPersistenceMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean markAsReadIfUnread(
            final Long notificationId,
            final Long recipientUserId,
            final LocalDateTime readAt
    ) {
        return jpaRepository.markAsReadIfUnread(notificationId, recipientUserId, readAt) > 0;
    }
}
