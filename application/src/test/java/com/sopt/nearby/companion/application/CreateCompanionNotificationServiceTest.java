// 동행 알림 생성 서비스의 중복 방지와 생성 값을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreateCompanionNotificationServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T17:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 17, 0);

    @Test
    void createsUnreadNotificationWhenUniqueKeyIsMissing() {
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        CreateCompanionNotificationService service = new CreateCompanionNotificationService(repository, CLOCK);

        CompanionNotification result = service.create(command(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L
        ));

        assertEquals(1L, result.id());
        assertEquals(7L, result.recipientUserId());
        assertEquals(CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED, result.notificationType());
        assertEquals(CompanionNotificationTargetType.COMPANION_APPLICATION, result.targetType());
        assertEquals(1L, result.targetId());
        assertNull(result.readAt());
        assertEquals(NOW, result.createdAt());
        assertEquals(1, repository.saveCount);
    }

    @Test
    void returnsExistingNotificationWhenSameEventAlreadyExists() {
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        CompanionNotification existing = new CompanionNotification(
                10L,
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                1L,
                null,
                NOW.minusHours(1)
        );
        repository.put(existing);
        CreateCompanionNotificationService service = new CreateCompanionNotificationService(repository, CLOCK);

        CompanionNotification result = service.create(command(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                1L
        ));

        assertEquals(existing, result);
        assertEquals(0, repository.saveCount);
    }

    private CreateCompanionNotificationCommand command(
            final Long recipientUserId,
            final CompanionNotificationType notificationType,
            final Long applicationId
    ) {
        return new CreateCompanionNotificationCommand(
                recipientUserId,
                notificationType,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId
        );
    }

    private static final class FakeCompanionNotificationRepository implements CompanionNotificationRepository {

        private final Map<Key, CompanionNotification> notifications = new HashMap<>();
        private long nextId = 1L;
        private int saveCount = 0;

        @Override
        public CompanionNotification save(final CompanionNotification model) {
            CompanionNotification saved = new CompanionNotification(
                    nextId++,
                    model.recipientUserId(),
                    model.notificationType(),
                    model.targetType(),
                    model.targetId(),
                    model.readAt(),
                    model.createdAt()
            );
            put(saved);
            saveCount++;
            return saved;
        }

        @Override
        public Optional<CompanionNotification> findById(final Long id) {
            return notifications.values().stream()
                    .filter(notification -> notification.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<CompanionNotification> findByUniqueKey(
                final CompanionNotificationType notificationType,
                final CompanionNotificationTargetType targetType,
                final Long targetId,
                final Long recipientUserId
        ) {
            return Optional.ofNullable(notifications.get(new Key(
                    notificationType,
                    targetType,
                    targetId,
                    recipientUserId
            )));
        }

        @Override
        public boolean markAsReadIfUnread(
                final Long notificationId,
                final Long recipientUserId,
                final LocalDateTime readAt
        ) {
            throw new UnsupportedOperationException("알림 생성 테스트에서는 읽음 처리를 사용하지 않습니다.");
        }

        private void put(final CompanionNotification notification) {
            notifications.put(new Key(
                    notification.notificationType(),
                    notification.targetType(),
                    notification.targetId(),
                    notification.recipientUserId()
            ), notification);
        }
    }

    private record Key(
            CompanionNotificationType notificationType,
            CompanionNotificationTargetType targetType,
            Long targetId,
            Long recipientUserId
    ) {
    }
}
