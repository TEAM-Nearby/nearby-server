// 동행 알림 읽음 처리 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionNotificationNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionNotificationException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationIdException;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.port.in.MarkCompanionNotificationAsReadUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import java.time.Clock;
import java.time.LocalDateTime;

public class MarkCompanionNotificationAsReadService implements MarkCompanionNotificationAsReadUseCase {

    private final CompanionNotificationRepository repository;
    private final Clock clock;

    public MarkCompanionNotificationAsReadService(
            final CompanionNotificationRepository repository,
            final Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public MarkCompanionNotificationAsReadResult markAsRead(final Long userId, final Long notificationId) {
        if (notificationId == null || notificationId <= 0) {
            throw new InvalidCompanionNotificationIdException();
        }

        CompanionNotification notification = repository.findById(notificationId)
                .orElseThrow(CompanionNotificationNotFoundException::new);

        if (!notification.recipientUserId().equals(userId)) {
            throw new ForbiddenCompanionNotificationException();
        }

        if (notification.isRead()) {
            return toResult(notification);
        }

        LocalDateTime readAt = LocalDateTime.now(clock);
        boolean marked = repository.markAsReadIfUnread(notificationId, userId, readAt);
        if (marked) {
            return new MarkCompanionNotificationAsReadResult(notificationId, true, readAt);
        }

        CompanionNotification currentNotification = repository.findById(notificationId)
                .orElseThrow(CompanionNotificationNotFoundException::new);
        if (!currentNotification.recipientUserId().equals(userId)) {
            throw new ForbiddenCompanionNotificationException();
        }
        return toResult(currentNotification);
    }

    private MarkCompanionNotificationAsReadResult toResult(final CompanionNotification notification) {
        return new MarkCompanionNotificationAsReadResult(
                notification.id(),
                notification.isRead(),
                notification.readAt()
        );
    }
}
