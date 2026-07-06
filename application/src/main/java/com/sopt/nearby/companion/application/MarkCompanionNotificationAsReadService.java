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
            return new MarkCompanionNotificationAsReadResult(
                    notification.id(),
                    true,
                    notification.readAt()
            );
        }

        CompanionNotification readNotification = notification.markAsRead(LocalDateTime.now(clock));
        CompanionNotification savedNotification = repository.save(readNotification);

        return new MarkCompanionNotificationAsReadResult(
                savedNotification.id(),
                savedNotification.isRead(),
                savedNotification.readAt()
        );
    }
}