// 동행 푸시 발송 작업의 배치 결과와 재시도 규칙을 검증한다.
package com.sopt.nearby.companion.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDeliveryStatus;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessCompanionPushDeliveriesServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-09-15T00:00:00Z"), ZoneOffset.UTC);
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 15, 0, 0);

    @Test
    void marksSuccessfulBatchAsSent() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.SENT);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
        );

        service.processBatch();

        assertEquals(1, sender.messages.size());
        assertEquals(50, repository.cleanupLimit);
        assertEquals(5, repository.recoveredMaxAttempts);
        assertEquals(CompanionPushDeliveryStatus.SENT, repository.status);
        assertEquals("provider-1", repository.providerMessageId);
        assertNotNull(sender.messages.get(0).data().get("notificationId"));
    }

    @Test
    void schedulesOnlyRetryableFailure() {
        FakeRepository repository = new FakeRepository(delivery());
        CapturingSender sender = new CapturingSender(PushDeliveryResult.Outcome.RETRYABLE_FAILURE);
        ProcessCompanionPushDeliveriesService service = new ProcessCompanionPushDeliveriesService(
                repository, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
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
                repository, endpointRepository, sender, CLOCK, 50, 5, Duration.ofSeconds(30)
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

    private CompanionPushDelivery delivery() {
        return new CompanionPushDelivery(
                1L,
                2L,
                3L,
                1L,
                7L,
                "token",
                "title",
                "body",
                CompanionNotificationTargetType.COMPANION_APPLICATION,
                9L,
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
        public CompanionPushEndpoint save(CompanionPushEndpoint endpoint) {
            return endpoint;
        }
    }

    private static final class FakeRepository implements CompanionPushDeliveryRepository {

        private final CompanionPushDelivery delivery;
        private CompanionPushDeliveryStatus status;
        private LocalDateTime nextAttemptAt;
        private String errorCode;
        private String providerMessageId;
        private int cleanupLimit;
        private int recoveredMaxAttempts;

        private FakeRepository(CompanionPushDelivery delivery) {
            this.delivery = delivery;
            this.status = delivery.status();
            this.nextAttemptAt = delivery.nextAttemptAt();
        }

        @Override
        public CompanionPushDelivery saveIfAbsent(CompanionPushDelivery delivery) { return delivery; }

        @Override
        public List<CompanionPushDelivery> claimDue(int batchSize, LocalDateTime now,
                                                     LocalDateTime leaseUntil, String claimToken) {
            return List.of(new CompanionPushDelivery(
                    delivery.id(), delivery.notificationId(), delivery.endpointId(),
                    delivery.endpointRegistrationVersion(), delivery.recipientUserId(),
                    delivery.token(), delivery.title(), delivery.body(), delivery.targetType(), delivery.targetId(),
                    CompanionPushDeliveryStatus.PROCESSING, 1, delivery.nextAttemptAt(), leaseUntil, claimToken,
                    delivery.providerMessageId(), delivery.lastErrorCode(), delivery.expiresAt(),
                    delivery.createdAt(), now
            ));
        }

        @Override
        public boolean markSent(Long deliveryId, String claimToken, String providerMessageId, LocalDateTime now) {
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
}
