// 동행 알림 읽음 처리 서비스의 검증과 멱등 처리를 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.companion.domain.exception.CompanionNotificationNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionNotificationException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationIdException;
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

class MarkCompanionNotificationAsReadServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-06T17:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 6, 17, 0);

    @Test
    void marksUnreadNotificationAsRead() {
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        repository.put(notification(1L, 7L, null));
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                repository,
                CLOCK
        );

        MarkCompanionNotificationAsReadResult result = service.markAsRead(7L, 1L);

        assertEquals(1L, result.notificationId());
        assertEquals(true, result.isRead());
        assertEquals(NOW, result.readAt());
        assertEquals(1, repository.markAsReadIfUnreadCount);
        assertEquals(NOW, repository.findById(1L).orElseThrow().readAt());
    }

    @Test
    void keepsExistingReadAtWhenNotificationIsAlreadyRead() {
        LocalDateTime readAt = NOW.minusHours(1);
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        repository.put(notification(1L, 7L, readAt));
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                repository,
                CLOCK
        );

        MarkCompanionNotificationAsReadResult result = service.markAsRead(7L, 1L);

        assertEquals(1L, result.notificationId());
        assertEquals(true, result.isRead());
        assertEquals(readAt, result.readAt());
        assertEquals(0, repository.markAsReadIfUnreadCount);
    }

    @Test
    void returnsExistingReadAtWhenConditionalUpdateLosesRace() {
        LocalDateTime firstReadAt = NOW.minusMinutes(5);
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        repository.put(notification(1L, 7L, null));
        repository.concurrentReadAt = firstReadAt;
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                repository,
                CLOCK
        );

        MarkCompanionNotificationAsReadResult result = service.markAsRead(7L, 1L);

        assertEquals(1L, result.notificationId());
        assertEquals(true, result.isRead());
        assertEquals(firstReadAt, result.readAt());
        assertEquals(1, repository.markAsReadIfUnreadCount);
    }

    @Test
    void rejectsInvalidNotificationId() {
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                new FakeCompanionNotificationRepository(),
                CLOCK
        );

        assertThrows(InvalidCompanionNotificationIdException.class, () -> service.markAsRead(7L, 0L));
        assertThrows(InvalidCompanionNotificationIdException.class, () -> service.markAsRead(7L, null));
    }

    @Test
    void rejectsMissingNotification() {
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                new FakeCompanionNotificationRepository(),
                CLOCK
        );

        assertThrows(CompanionNotificationNotFoundException.class, () -> service.markAsRead(7L, 1L));
    }

    @Test
    void rejectsNotificationOwnedByAnotherUser() {
        FakeCompanionNotificationRepository repository = new FakeCompanionNotificationRepository();
        repository.put(notification(1L, 99L, null));
        MarkCompanionNotificationAsReadService service = new MarkCompanionNotificationAsReadService(
                repository,
                CLOCK
        );

        assertThrows(ForbiddenCompanionNotificationException.class, () -> service.markAsRead(7L, 1L));
        assertEquals(0, repository.markAsReadIfUnreadCount);
    }

    private CompanionNotification notification(
            final Long id,
            final Long recipientUserId,
            final LocalDateTime readAt
    ) {
        return new CompanionNotification(
                id,
                recipientUserId,
                CompanionNotificationType.COMPANION_APPLICATION_ACCEPTED,
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                100L,
                readAt,
                NOW.minusDays(1)
        );
    }

    private static final class FakeCompanionNotificationRepository implements CompanionNotificationRepository {

        private final Map<Long, CompanionNotification> notifications = new HashMap<>();
        private LocalDateTime concurrentReadAt;
        private int markAsReadIfUnreadCount = 0;

        @Override
        public CompanionNotification save(final CompanionNotification model) {
            notifications.put(model.id(), model);
            return model;
        }

        @Override
        public Optional<CompanionNotification> findById(final Long id) {
            return Optional.ofNullable(notifications.get(id));
        }

        @Override
        public Optional<CompanionNotification> findByUniqueKey(
                final CompanionNotificationType notificationType,
                final CompanionNotificationTargetType targetType,
                final Long targetId,
                final Long recipientUserId
        ) {
            return notifications.values().stream()
                    .filter(notification -> notification.notificationType().equals(notificationType))
                    .filter(notification -> notification.targetType().equals(targetType))
                    .filter(notification -> notification.targetId().equals(targetId))
                    .filter(notification -> notification.recipientUserId().equals(recipientUserId))
                    .findFirst();
        }

        @Override
        public boolean markAsReadIfUnread(
                final Long notificationId,
                final Long recipientUserId,
                final LocalDateTime readAt
        ) {
            markAsReadIfUnreadCount++;
            CompanionNotification notification = notifications.get(notificationId);
            if (notification == null || !notification.recipientUserId().equals(recipientUserId)) {
                return false;
            }
            if (concurrentReadAt != null) {
                notifications.put(notificationId, notification.markAsRead(concurrentReadAt));
                return false;
            }
            if (notification.isRead()) {
                return false;
            }
            notifications.put(notificationId, notification.markAsRead(readAt));
            return true;
        }

        private void put(final CompanionNotification notification) {
            notifications.put(notification.id(), notification);
        }
    }
}
