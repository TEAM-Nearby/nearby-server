// 동행 알림 생성 서비스의 중복 방지와 생성 값을 검증하는 테스트
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    @Test
    void createsDeliveryForEachActiveEndpointWhenNotificationIsNew() {
        FakeCompanionNotificationRepository notificationRepository = new FakeCompanionNotificationRepository();
        FakePushEndpointRepository endpointRepository = new FakePushEndpointRepository(List.of(
                endpoint(11L, true),
                endpoint(12L, true),
                endpoint(13L, false)
        ));
        CapturingPushDeliveryRepository deliveryRepository = new CapturingPushDeliveryRepository();
        CreateCompanionNotificationService service = new CreateCompanionNotificationService(
                notificationRepository,
                endpointRepository,
                deliveryRepository,
                CLOCK
        );

        service.create(command(
                7L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED,
                1L
        ));

        assertEquals(2, deliveryRepository.deliveries.size());
        assertEquals("새로운 동행 신청", deliveryRepository.deliveries.get(0).title());
        assertEquals("새로운 동행 신청이 도착했어요.", deliveryRepository.deliveries.get(0).body());
        assertEquals(11L, deliveryRepository.deliveries.get(0).endpointId());
        assertEquals(12L, deliveryRepository.deliveries.get(1).endpointId());
    }

    private CompanionPushEndpoint endpoint(final Long id, final boolean active) {
        return new CompanionPushEndpoint(
                id,
                7L,
                "installation-" + id,
                "token-" + id,
                CompanionPushPlatform.ANDROID,
                active,
                1L,
                NOW,
                NOW,
                NOW
        );
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

    private static final class FakePushEndpointRepository implements CompanionPushEndpointRepository {

        private final List<CompanionPushEndpoint> endpoints;

        private FakePushEndpointRepository(final List<CompanionPushEndpoint> endpoints) {
            this.endpoints = endpoints;
        }

        @Override
        public Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(
                final Long userId,
                final String installationId
        ) {
            return endpoints.stream()
                    .filter(endpoint -> endpoint.userId().equals(userId)
                            && endpoint.installationId().equals(installationId))
                    .findFirst();
        }

        @Override
        public List<CompanionPushEndpoint> findActiveByUserId(final Long userId) {
            return endpoints.stream()
                    .filter(endpoint -> endpoint.userId().equals(userId) && endpoint.active())
                    .toList();
        }

        @Override
        public int deactivateByTokenExceptUser(
                final String token,
                final Long userId,
                final LocalDateTime now
        ) {
            return 0;
        }

        @Override
        public int deactivateById(final Long endpointId, final LocalDateTime now) {
            return 0;
        }

        @Override
        public CompanionPushEndpoint upsert(
                final Long userId,
                final String installationId,
                final String token,
                final CompanionPushPlatform platform,
                final LocalDateTime now
        ) {
            return findByUserIdAndInstallationId(userId, installationId)
                    .orElseThrow(() -> new IllegalStateException("테스트에서 호출하지 않는 등록 연산입니다."));
        }

        @Override
        public CompanionPushEndpoint save(final CompanionPushEndpoint endpoint) {
            return endpoint;
        }
    }

    private static final class CapturingPushDeliveryRepository implements CompanionPushDeliveryRepository {

        private final List<CompanionPushDelivery> deliveries = new ArrayList<>();

        @Override
        public CompanionPushDelivery saveIfAbsent(final CompanionPushDelivery delivery) {
            deliveries.add(delivery);
            return delivery;
        }

        @Override
        public List<CompanionPushDelivery> claimDue(
                final int batchSize,
                final LocalDateTime now,
                final LocalDateTime leaseUntil,
                final String claimToken
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean markSent(
                final Long deliveryId,
                final String claimToken,
                final String providerMessageId,
                final LocalDateTime now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean markRetry(
                final Long deliveryId,
                final String claimToken,
                final LocalDateTime nextAttemptAt,
                final String errorCode,
                final LocalDateTime now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean markPermanentFailure(
                final Long deliveryId,
                final String claimToken,
                final String errorCode,
                final LocalDateTime now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean markExpired(
                final Long deliveryId,
                final String claimToken,
                final String errorCode,
                final LocalDateTime now
        ) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int skipInactiveEndpointDeliveries(final LocalDateTime now, final int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int expireExpiredDeliveries(final LocalDateTime now, final int limit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int recoverExpiredLeases(final LocalDateTime now, final int maxAttempts, final int limit) {
            throw new UnsupportedOperationException();
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
