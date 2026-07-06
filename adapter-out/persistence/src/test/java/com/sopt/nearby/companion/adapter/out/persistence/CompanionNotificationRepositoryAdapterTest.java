// 동행 알림 저장소 어댑터의 저장, 조회, 중복 제약을 검증하는 테스트
package com.sopt.nearby.companion.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionNotificationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionNotificationStoreJpaRepository;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
class CompanionNotificationRepositoryAdapterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 17, 0);

    @Autowired
    private CompanionNotificationStoreJpaRepository jpaRepository;

    @Test
    void savesNotificationAndFindsByUniqueKey() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);

        CompanionNotification saved = adapter.save(notification(
                null,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                null,
                NOW
        ));

        assertThat(saved.id()).isNotNull();
        assertThat(adapter.findById(saved.id())).get().isEqualTo(saved);
        assertThat(adapter.findByUniqueKey(
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                1L,
                7L
        )).get().isEqualTo(saved);
    }

    @Test
    void returnsEmptyWhenUniqueKeyDoesNotExist() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);

        assertThat(adapter.findByUniqueKey(
                CompanionNotificationType.COMPANION_APPLICATION_REJECTED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                99L,
                7L
        )).isEmpty();
    }

    @Test
    void marksAsReadOnlyWhenUnreadNotificationBelongsToRecipient() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);
        CompanionNotification saved = adapter.save(notification(
                null,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                null,
                NOW
        ));

        boolean updated = adapter.markAsReadIfUnread(saved.id(), 7L, NOW.plusMinutes(30));

        assertThat(updated).isTrue();
        assertThat(adapter.findById(saved.id())).get()
                .extracting(CompanionNotification::readAt)
                .isEqualTo(NOW.plusMinutes(30));
    }

    @Test
    void doesNotOverwriteReadAtWhenNotificationIsAlreadyRead() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);
        CompanionNotification saved = adapter.save(notification(
                null,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                NOW.plusMinutes(10),
                NOW
        ));

        boolean updated = adapter.markAsReadIfUnread(saved.id(), 7L, NOW.plusMinutes(30));

        assertThat(updated).isFalse();
        assertThat(adapter.findById(saved.id())).get()
                .extracting(CompanionNotification::readAt)
                .isEqualTo(NOW.plusMinutes(10));
    }

    @Test
    void doesNotMarkAsReadWhenRecipientDoesNotMatch() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);
        CompanionNotification saved = adapter.save(notification(
                null,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                null,
                NOW
        ));

        boolean updated = adapter.markAsReadIfUnread(saved.id(), 99L, NOW.plusMinutes(30));

        assertThat(updated).isFalse();
        assertThat(adapter.findById(saved.id())).get()
                .extracting(CompanionNotification::readAt)
                .isNull();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void returnsExistingNotificationWhenSaveConflictsWithUniqueKey() {
        CompanionNotificationRepositoryAdapter adapter = new CompanionNotificationRepositoryAdapter(jpaRepository);
        CompanionNotificationEntity existing = jpaRepository.saveAndFlush(entity(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1001L,
                null,
                NOW
        ));

        CompanionNotification result = adapter.save(notification(
                null,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1001L,
                null,
                NOW.plusMinutes(1)
        ));

        assertThat(result.id()).isEqualTo(existing.getId());
        assertThat(result.createdAt()).isEqualTo(NOW);
        assertThat(jpaRepository.findByNotificationTypeAndTargetTypeAndTargetIdAndRecipientUserId(
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                1001L,
                7L
        )).get().extracting(CompanionNotificationEntity::getId).isEqualTo(existing.getId());
    }

    @Test
    void enforcesUniqueNotificationEventPerRecipient() {
        jpaRepository.saveAndFlush(entity(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                null,
                NOW
        ));

        assertThatThrownBy(() -> jpaRepository.saveAndFlush(entity(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L,
                null,
                NOW.plusMinutes(1)
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    private CompanionNotification notification(
            final Long id,
            final Long recipientUserId,
            final CompanionNotificationType notificationType,
            final Long applicationId,
            final LocalDateTime readAt,
            final LocalDateTime createdAt
    ) {
        return new CompanionNotification(
                id,
                recipientUserId,
                notificationType,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId,
                readAt,
                createdAt
        );
    }

    private CompanionNotificationEntity entity(
            final Long recipientUserId,
            final CompanionNotificationType notificationType,
            final Long applicationId,
            final LocalDateTime readAt,
            final LocalDateTime createdAt
    ) {
        return new CompanionNotificationEntity(
                null,
                recipientUserId,
                notificationType,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId,
                readAt,
                createdAt
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = CompanionNotificationEntity.class)
    @EnableJpaRepositories(basePackageClasses = CompanionNotificationStoreJpaRepository.class)
    static class TestApplication {
    }
}
