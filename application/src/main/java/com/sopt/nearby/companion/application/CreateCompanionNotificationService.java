// 동행 알림을 중복 없이 생성하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import java.time.Clock;
import java.time.LocalDateTime;

public class CreateCompanionNotificationService implements CreateCompanionNotificationUseCase {

    private final CompanionNotificationRepository repository;
    private final Clock clock;

    public CreateCompanionNotificationService(
            final CompanionNotificationRepository repository,
            final Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public CompanionNotification create(final CreateCompanionNotificationCommand command) {
        return repository.findByUniqueKey(
                        command.notificationType(),
                        command.targetType(),
                        command.targetId(),
                        command.recipientUserId()
                )
                .orElseGet(() -> repository.save(new CompanionNotification(
                        null,
                        command.recipientUserId(),
                        command.notificationType(),
                        command.targetType(),
                        command.targetId(),
                        null,
                        LocalDateTime.now(clock)
                )));
    }
}
