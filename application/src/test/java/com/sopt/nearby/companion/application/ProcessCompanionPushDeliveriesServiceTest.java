// 동행 푸시 발송 작업의 배치 결과와 재시도 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDeliveryStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import com.sopt.nearby.companion.port.out.PushSender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProcessCompanionPushDeliveriesServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 0, 0);

    @ParameterizedTest
    @EnumSource(CompanionNotificationType.class)
    void sendsOriginalEventTypeAndApplicationIdForEveryNotificationType(final CompanionNotificationType type) {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, new FakeNotificationRepository(notification(2L, 9L, type)),
                sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(1, sender.messages.size());
        assertEquals(50, repository.cleanupLimit);
        assertEquals(5, repository.recoveredMaxAttempts);
        assertEquals(CompanionPushDeliveryStatus.SENT, repository.status);
        assertEquals("provider-1", repository.providerMessageId);
        PushMessage message = sender.messages.get(0);
        assertEquals(data(2L, 9L, type), message.data());
        assertEquals("title", message.title());
        assertEquals("body", message.body());
        assertEquals("token", message.token());
        assertEquals(NOW.plusHours(24), message.expiresAt());
    }

    @Test
    void schedulesOnlyRetryableFailure() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.RETRYABLE_FAILURE);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, notifications(), sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(CompanionPushDeliveryStatus.RETRY, repository.status);
        assertEquals("TEMPORARY", repository.errorCode);
        assertEquals(NOW.plusSeconds(2), repository.nextAttemptAt);
    }

    @Test
    void deactivatesEndpointWhenProviderRejectsUnregisteredToken() {
        FakeRepository repository = new FakeRepository(delivery());
        FakeEndpointRepository endpointRepository = new FakeEndpointRepository();
        CapturingSender sender = new CapturingSender(
                PushDeliveryResult.Outcome.PERMANENT_FAILURE,
                "UNREGISTERED"
        );
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, notifications(), endpointRepository, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(3L, endpointRepository.deactivatedEndpointId);
        assertEquals(1L, endpointRepository.registrationVersion);
        assertEquals("token", endpointRepository.token);
    }

    @Test
    void appliesProviderDelayAndJitterToRetrySchedule() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(
                PushDeliveryResult.Outcome.RETRYABLE_FAILURE,
                "QUOTA_EXCEEDED",
                90L
        );
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository,
                notifications(),
                null,
                sender,
                CLOCK,
                50,
                5,
                Duration.ofSeconds(30),
                () -> 3
        );

        service.processBatch();

        assertEquals(NOW.plusSeconds(93), repository.nextAttemptAt);
    }

    @Test
    void capsProviderDelayBeforeAddingJitter() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(
                PushDeliveryResult.Outcome.RETRYABLE_FAILURE,
                "TEMPORARY",
                Long.MAX_VALUE
        );
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository,
                notifications(),
                null,
                sender,
                CLOCK,
                50,
                5,
                Duration.ofSeconds(30),
                () -> 5
        );

        service.processBatch();

        assertEquals(NOW.plusHours(24), repository.nextAttemptAt);
    }

    @Test
    void looksUpDistinctNotificationsOnceAndSharesDataAcrossDevices() {
        FakeRepository repository = new FakeRepository(
                delivery(), delivery(4L, 2L, 5L, 9L), delivery(6L, 8L, 3L, 10L)
        );
        FakeNotificationRepository notifications = new FakeNotificationRepository(
                notification(8L, 10L, CompanionNotificationType.COMPANION_APPLICATION_REJECTED),
                notification(2L, 9L, CompanionNotificationType.COMPANION_APPLICATION_CREATED)
        );
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, notifications, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(List.of(List.of(2L, 8L)), notifications.requests);
        assertEquals(1, sender.sendCount);
        assertEquals(3, sender.messages.size());
        assertEquals(data(2L, 9L, CompanionNotificationType.COMPANION_APPLICATION_CREATED),
                sender.messages.get(0).data());
        assertEquals(sender.messages.get(0).data(), sender.messages.get(1).data());
        assertEquals(data(8L, 10L, CompanionNotificationType.COMPANION_APPLICATION_REJECTED),
                sender.messages.get(2).data());
        assertEquals(List.of(1L, 4L, 6L), repository.sentIds);
    }

    @ParameterizedTest
    @EnumSource(CompanionNotificationType.class)
    void preservesRoutingDataWhenRetryingTheSameNotification(final CompanionNotificationType type) {
        FakeRepository repository = new FakeRepository(delivery());
        FakeNotificationRepository notifications = new FakeNotificationRepository(notification(2L, 9L, type));
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.RETRYABLE_FAILURE);
        ProcessCompanionPushDeliveriesService firstAttempt = new ProcessCompanionPushDeliveriesService(
                repository, notifications, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );
        firstAttempt.processBatch();
        assertEquals(CompanionPushDeliveryStatus.RETRY, repository.status);
        ProcessCompanionPushDeliveriesService retry = new ProcessCompanionPushDeliveriesService(
                repository, notifications, sender, Clock.offset(CLOCK, Duration.ofSeconds(2)),
                50, 5, Duration.ofSeconds(30)
        );

        retry.processBatch();

        assertEquals(2, sender.sendCount);
        assertEquals(data(2L, 9L, type), sender.messages.get(0).data());
        assertEquals(sender.messages.get(0).data(), sender.messages.get(1).data());
        assertEquals(List.of(List.of(2L), List.of(2L)), notifications.requests);
        assertEquals(NOW.plusSeconds(6), repository.nextAttemptAt);
    }

    @Test
    void failsOnlyDeliveriesWhoseOriginalNotificationIsMissing() {
        FakeRepository repository = new FakeRepository(delivery(), delivery(4L, 99L, 5L, 10L));
        FakeEndpointRepository endpoints = new FakeEndpointRepository();
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, notifications(), endpoints, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(Map.of(4L, "NOTIFICATION_NOT_FOUND"), repository.failures);
        assertEquals(List.of(1L), repository.sentIds);
        assertEquals(List.of(1L), sender.messages.stream().map(PushMessage::deliveryId).toList());
        assertNull(endpoints.deactivatedEndpointId);
    }

    @Test
    void doesNotCallProviderWhenEveryOriginalNotificationIsMissing() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, new FakeNotificationRepository(), sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(Map.of(1L, "NOTIFICATION_NOT_FOUND"), repository.failures);
        assertEquals(0, sender.sendCount);
        assertEquals(CompanionPushDeliveryStatus.FAILED_PERMANENT, repository.status);
    }

    @Test
    void doesNotLookUpNotificationsOrCallProviderWhenNoDeliveryIsDue() {
        FakeNotificationRepository notifications = notifications();
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                new FakeRepository(), notifications, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(List.of(), notifications.requests);
        assertEquals(0, sender.sendCount);
    }

    private FakeNotificationRepository notifications() {
        return new FakeNotificationRepository(notification(2L, 9L,
                CompanionNotificationType.COMPANION_APPLICATION_CREATED));
    }

    private CompanionNotification notification(
            final Long id, final Long applicationId, final CompanionNotificationType type
    ) {
        return new CompanionNotification(id, 7L, type, CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId, null, NOW);
    }

    private Map<String, String> data(
            final Long notificationId, final Long applicationId, final CompanionNotificationType type
    ) {
        return Map.of(
                "type", type.name(),
                "applicationId", applicationId.toString(),
                "notificationId", notificationId.toString(),
                "targetType", "COMPANION_APPLICATION",
                "targetId", applicationId.toString()
        );
    }

    private CompanionPushDelivery delivery() {
        return delivery(1L, 2L, 3L, 9L);
    }

    private CompanionPushDelivery delivery(
            final Long id, final Long notificationId, final Long endpointId, final Long applicationId
    ) {
        return new CompanionPushDelivery(
                id,
                notificationId,
                endpointId,
                1L,
                7L,
                "token",
                "title",
                "body",
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                applicationId,
                CompanionPushDeliveryStatus.PENDING,
                0,
                NOW,
                null,
                null,
                null,
                null,
                NOW.plusHours(24),
                NOW,
                NOW
        );
    }

    private static final class CapturingSender implements PushSender {

        private final PushDeliveryResult.Outcome outcome;
        private final String errorCode;
        private final Long retryAfterSeconds;
        private final List<PushMessage> messages = new ArrayList<>();
        private int sendCount;

        private CapturingSender(PushDeliveryResult.Outcome outcome) {
            this(outcome, "TEMPORARY", null);
        }

        private CapturingSender(PushDeliveryResult.Outcome outcome, String errorCode) {
            this(outcome, errorCode, null);
        }

        private CapturingSender(
                PushDeliveryResult.Outcome outcome,
                String errorCode,
                Long retryAfterSeconds
        ) {
            this.outcome = outcome;
            this.errorCode = errorCode;
            this.retryAfterSeconds = retryAfterSeconds;
        }

        @Override
        public List<PushDeliveryResult> send(List<PushMessage> messages) {
            sendCount++;
            this.messages.addAll(messages);
            return messages.stream().map(message -> new PushDeliveryResult(
                    message.deliveryId(),
                    outcome,
                    outcome == PushDeliveryResult.Outcome.SENT ? "provider-1" : null,
                    outcome == PushDeliveryResult.Outcome.SENT ? null : errorCode,
                    retryAfterSeconds
            )).toList();
        }
    }

    private static final class FakeEndpointRepository implements CompanionPushEndpointRepository {

        private Long deactivatedEndpointId;
        private long registrationVersion;
        private String token;

        @Override
        public int deactivateIfCurrent(
                final Long endpointId,
                final long registrationVersion,
                final String token,
                final LocalDateTime now
        ) {
            this.deactivatedEndpointId = endpointId;
            this.registrationVersion = registrationVersion;
            this.token = token;
            return 1;
        }

        @Override
        public Optional<CompanionPushEndpoint> findByUserIdAndInstallationId(Long userId, String installationId) {
            return Optional.empty();
        }

        @Override
        public List<CompanionPushEndpoint> findActiveByUserId(Long userId) {
            return List.of();
        }

        @Override
        public int deactivateByTokenExceptUser(String token, Long userId, LocalDateTime now) {
            return 0;
        }

        @Override
        public int deactivateById(Long endpointId, LocalDateTime now) {
            deactivatedEndpointId = endpointId;
            return 1;
        }

        @Override
        public CompanionPushEndpoint upsert(
                final Long userId,
                final String installationId,
                final String token,
                final CompanionPushPlatform platform,
                final LocalDateTime now
        ) {
            throw new UnsupportedOperationException("테스트에서 호출하지 않는 등록 연산입니다.");
        }

        @Override
        public CompanionPushEndpoint save(CompanionPushEndpoint endpoint) {
            return endpoint;
        }
    }

    private static final class FakeRepository implements CompanionPushDeliveryRepository {

        private final List<CompanionPushDelivery> deliveries;
        private final Map<Long, String> failures = new HashMap<>();
        private final List<Long> sentIds = new ArrayList<>();
        private int claimCount;
        private CompanionPushDeliveryStatus status;
        private LocalDateTime nextAttemptAt;
        private String errorCode;
        private String providerMessageId;
        private int cleanupLimit;
        private int recoveredMaxAttempts;

        private FakeRepository(CompanionPushDelivery... deliveries) {
            this.deliveries = List.of(deliveries);
        }

        @Override
        public CompanionPushDelivery saveIfAbsent(CompanionPushDelivery delivery) { return delivery; }

        @Override
        public List<CompanionPushDelivery> claimDue(int batchSize, LocalDateTime now,
                                                     LocalDateTime leaseUntil, String claimToken) {
            claimCount++;
            return deliveries.stream().map(delivery -> new CompanionPushDelivery(
                    delivery.id(), delivery.notificationId(), delivery.endpointId(),
                    delivery.endpointRegistrationVersion(), delivery.recipientUserId(),
                    delivery.token(), delivery.title(), delivery.body(), delivery.targetType(), delivery.targetId(),
                    CompanionPushDeliveryStatus.PROCESSING, claimCount, delivery.nextAttemptAt(), leaseUntil, claimToken,
                    delivery.providerMessageId(), delivery.lastErrorCode(), delivery.expiresAt(),
                    delivery.createdAt(), now
            )).toList();
        }

        @Override
        public boolean markSent(Long deliveryId, String claimToken, String providerMessageId, LocalDateTime now) {
            sentIds.add(deliveryId);
            status = CompanionPushDeliveryStatus.SENT;
            this.providerMessageId = providerMessageId;
            return true;
        }

        @Override
        public boolean markRetry(Long deliveryId, String claimToken, LocalDateTime nextAttemptAt,
                                 String errorCode, LocalDateTime now) {
            status = CompanionPushDeliveryStatus.RETRY;
            this.nextAttemptAt = nextAttemptAt;
            this.errorCode = errorCode;
            return true;
        }

        @Override
        public boolean markPermanentFailure(Long deliveryId, String claimToken, String errorCode, LocalDateTime now) {
            failures.put(deliveryId, errorCode);
            status = CompanionPushDeliveryStatus.FAILED_PERMANENT;
            return true;
        }

        @Override
        public boolean markExpired(Long deliveryId, String claimToken, String errorCode, LocalDateTime now) {
            status = CompanionPushDeliveryStatus.EXPIRED;
            return true;
        }

        @Override
        public int recoverExpiredLeases(LocalDateTime now, int maxAttempts, int limit) {
            recoveredMaxAttempts = maxAttempts;
            cleanupLimit = limit;
            return 0;
        }

        @Override
        public int expireExpiredDeliveries(LocalDateTime now, int limit) {
            cleanupLimit = limit;
            return 0;
        }

        @Override
        public int skipInactiveEndpointDeliveries(LocalDateTime now, int limit) {
            cleanupLimit = limit;
            return 0;
        }
    }

    private static final class FakeNotificationRepository implements CompanionNotificationRepository {

        private final List<CompanionNotification> notifications;
        private final List<List<Long>> requests = new ArrayList<>();

        private FakeNotificationRepository(CompanionNotification... notifications) {
            this.notifications = List.of(notifications);
        }

        @Override
        public List<CompanionNotification> findAllByIds(List<Long> notificationIds) {
            requests.add(List.copyOf(notificationIds));
            return notifications.stream().filter(notification -> notificationIds.contains(notification.id())).toList();
        }

        @Override
        public Optional<CompanionNotification> findById(Long id) {
            throw new AssertionError("발송 배치에서는 알림별 단건 조회를 사용하지 않습니다.");
        }

        @Override
        public CompanionNotification save(CompanionNotification model) {
            throw new AssertionError("발송 배치에서는 원본 알림을 변경하지 않습니다.");
        }

        @Override
        public Optional<CompanionNotification> findByUniqueKey(
                CompanionNotificationType type, CompanionNotificationTargetType targetType,
                Long targetId, Long recipientUserId
        ) {
            throw new AssertionError("발송 배치에서는 알림 식별자로 조회합니다.");
        }

        @Override
        public boolean markAsReadIfUnread(Long notificationId, Long recipientUserId, LocalDateTime readAt) {
            throw new AssertionError("발송 배치에서는 읽음 상태를 변경하지 않습니다.");
        }
    }
}
