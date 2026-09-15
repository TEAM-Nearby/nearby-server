// 동행 알림을 중복 없이 생성하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class CreateCompanionNotificationService implements CreateCompanionNotificationUseCase {

    private final CompanionNotificationRepository repository;
    private final CompanionPushEndpointRepository endpointRepository;
    private final CompanionPushDeliveryRepository deliveryRepository;
    private final Clock clock;

    public CreateCompanionNotificationService(
            final CompanionNotificationRepository repository,
            final Clock clock
    ) {
        this(repository, null, null, clock);
    }

    public CreateCompanionNotificationService(
            final CompanionNotificationRepository repository,
            final CompanionPushEndpointRepository endpointRepository,
            final CompanionPushDeliveryRepository deliveryRepository,
            final Clock clock
    ) {
        this.repository = repository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CompanionNotification create(final CreateCompanionNotificationCommand command) {
        Optional<CompanionNotification> existingNotification = repository.findByUniqueKey(
                        command.notificationType(),
                        command.targetType(),
                        command.targetId(),
                        command.recipientUserId()
                );
        if (existingNotification.isPresent()) {
            return existingNotification.get();
        }

        CompanionNotification notification = repository.save(new CompanionNotification(
                        null,
                        command.recipientUserId(),
                        command.notificationType(),
                        command.targetType(),
                        command.targetId(),
                        null,
                        LocalDateTime.now(clock)
                ));
        createPushDeliveries(notification);
        return notification;
    }

    private void createPushDeliveries(final CompanionNotification notification) {
        if (endpointRepository == null || deliveryRepository == null || notification.id() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        endpointRepository.findActiveByUserId(notification.recipientUserId()).stream()
                .map(endpoint -> toDelivery(notification, endpoint, now))
                .forEach(deliveryRepository::saveIfAbsent);
    }

    private CompanionPushDelivery toDelivery(
            final CompanionNotification notification,
            final CompanionPushEndpoint endpoint,
            final LocalDateTime now
    ) {
        return CompanionPushDelivery.pending(
                notification,
                endpoint,
                title(notification),
                body(notification),
                now,
                now.plusHours(24)
        );
    }

    private String body(final CompanionNotification notification) {
        return switch (notification.notificationType()) {
            case COMPANION_APPLICATION_CREATED -> "새로운 동행 신청이 도착했어요.";
            case COMPANION_APPLICATION_ACCEPTED -> "동행 신청이 수락됐어요.";
            case COMPANION_APPLICATION_REJECTED -> "동행 신청 결과를 확인해 주세요.";
        };
    }

    private String title(final CompanionNotification notification) {
        return switch (notification.notificationType()) {
            case COMPANION_APPLICATION_CREATED -> "새로운 동행 신청";
            case COMPANION_APPLICATION_ACCEPTED, COMPANION_APPLICATION_REJECTED -> "동행 신청 결과";
        };
    }
}
