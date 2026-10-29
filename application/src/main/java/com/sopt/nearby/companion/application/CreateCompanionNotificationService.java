// 동행 알림을 중복 없이 생성하는 유스케이스 구현체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.port.in.CreateCompanionNotificationUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionPushContentQueryPort;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;

public class CreateCompanionNotificationService implements CreateCompanionNotificationUseCase {

    private final CompanionNotificationRepository repository;
    private final CompanionPushEndpointRepository endpointRepository;
    private final CompanionPushDeliveryRepository deliveryRepository;
    private final CompanionPushContentQueryPort contentQueryPort;
    private final Clock clock;

    public CreateCompanionNotificationService(
            final CompanionNotificationRepository repository,
            final Clock clock
    ) {
        this(repository, null, null, null, clock);
    }

    public CreateCompanionNotificationService(
            final CompanionNotificationRepository repository,
            final CompanionPushEndpointRepository endpointRepository,
            final CompanionPushDeliveryRepository deliveryRepository,
            final CompanionPushContentQueryPort contentQueryPort,
            final Clock clock
    ) {
        this.repository = repository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.contentQueryPort = contentQueryPort;
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
        List<CompanionPushEndpoint> endpoints = endpointRepository.findActiveByUserId(notification.recipientUserId());
        if (endpoints.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        String body = body(notification);
        endpoints.stream()
                .map(endpoint -> toDelivery(notification, endpoint, body, now))
                .forEach(deliveryRepository::saveIfAbsent);
    }

    private CompanionPushDelivery toDelivery(
            final CompanionNotification notification,
            final CompanionPushEndpoint endpoint,
            final String body,
            final LocalDateTime now
    ) {
        return CompanionPushDelivery.pending(
                notification,
                endpoint,
                title(notification),
                body,
                now,
                now.plusHours(24)
        );
    }

    private String body(final CompanionNotification notification) {
        return switch (notification.notificationType()) {
            case COMPANION_APPLICATION_CREATED -> contentQueryPort.findRecipientNickname(notification.recipientUserId())
                    .filter(nickname -> !nickname.isBlank())
                    .map(nickname -> nickname + "님과 동행을 원하는 분이 있어요. 지금 바로 확인해보세요!")
                    .orElse("새로운 동행 신청이 도착했어요.");
            case COMPANION_APPLICATION_ACCEPTED -> placeName(notification)
                    + " 동행이 수락되었어요. 지금 확인해보세요!";
            case COMPANION_APPLICATION_REJECTED -> placeName(notification)
                    + " 동행 요청이 거절되었어요. 아쉽지만 다른 동행을 찾아보세요!";
        };
    }

    private String placeName(final CompanionNotification notification) {
        return contentQueryPort.findPlaceNameByApplicationId(notification.targetId())
                .orElseThrow(() -> new IllegalStateException("동행 푸시의 장소명을 찾을 수 없습니다."));
    }

    private String title(final CompanionNotification notification) {
        return switch (notification.notificationType()) {
            case COMPANION_APPLICATION_CREATED -> "새로운 동행 신청";
            case COMPANION_APPLICATION_ACCEPTED, COMPANION_APPLICATION_REJECTED -> "동행 신청 결과";
        };
    }
}
