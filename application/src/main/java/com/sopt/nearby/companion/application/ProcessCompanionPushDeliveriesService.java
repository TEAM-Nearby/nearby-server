// 동행 푸시 발송 작업을 배치로 확보하고 결과를 반영하는 서비스다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotification;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.port.in.ProcessCompanionPushDeliveriesUseCase;
import com.sopt.nearby.companion.port.out.CompanionNotificationRepository;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import com.sopt.nearby.companion.port.out.PushSender;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntSupplier;

public class ProcessCompanionPushDeliveriesService implements ProcessCompanionPushDeliveriesUseCase {

    private final CompanionPushDeliveryRepository repository;
    private final CompanionNotificationRepository notificationRepository;
    private final CompanionPushEndpointRepository endpointRepository;
    private final PushSender sender;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration leaseDuration;
    private final IntSupplier jitterSecondsSupplier;

    public ProcessCompanionPushDeliveriesService(
            final CompanionPushDeliveryRepository repository,
            final CompanionNotificationRepository notificationRepository,
            final PushSender sender,
            final Clock clock,
            final int batchSize,
            final int maxAttempts,
            final Duration leaseDuration
    ) {
        this(repository, notificationRepository, null, sender, clock, batchSize, maxAttempts, leaseDuration, () -> 0);
    }

    public ProcessCompanionPushDeliveriesService(
            final CompanionPushDeliveryRepository repository,
            final CompanionNotificationRepository notificationRepository,
            final CompanionPushEndpointRepository endpointRepository,
            final PushSender sender,
            final Clock clock,
            final int batchSize,
            final int maxAttempts,
            final Duration leaseDuration
    ) {
        this(repository, notificationRepository, endpointRepository, sender, clock, batchSize, maxAttempts,
                leaseDuration, () -> 0);
    }

    public ProcessCompanionPushDeliveriesService(
            final CompanionPushDeliveryRepository repository,
            final CompanionNotificationRepository notificationRepository,
            final CompanionPushEndpointRepository endpointRepository,
            final PushSender sender,
            final Clock clock,
            final int batchSize,
            final int maxAttempts,
            final Duration leaseDuration,
            final IntSupplier jitterSecondsSupplier
    ) {
        this.repository = repository;
        this.notificationRepository = notificationRepository;
        this.endpointRepository = endpointRepository;
        this.sender = sender;
        this.clock = clock;
        this.batchSize = Math.min(Math.max(batchSize, 1), 500);
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.leaseDuration = leaseDuration.isNegative() || leaseDuration.isZero()
                ? Duration.ofSeconds(30)
                : leaseDuration;
        this.jitterSecondsSupplier = jitterSecondsSupplier == null ? () -> 0 : jitterSecondsSupplier;
    }

    @Override
    public void processBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.skipInactiveEndpointDeliveries(now, batchSize);
        repository.expireExpiredDeliveries(now, batchSize);
        repository.recoverExpiredLeases(now, maxAttempts, batchSize);

        String claimToken = UUID.randomUUID().toString();
        List<CompanionPushDelivery> deliveries = repository.claimDue(
                batchSize,
                now,
                now.plus(leaseDuration),
                claimToken
        );
        if (deliveries.isEmpty()) {
            return;
        }

        List<Long> notificationIds = deliveries.stream()
                .map(CompanionPushDelivery::notificationId)
                .distinct()
                .toList();
        Map<Long, CompanionNotification> notifications = new HashMap<>();
        for (CompanionNotification notification : notificationRepository.findAllByIds(notificationIds)) {
            notifications.put(notification.id(), notification);
        }

        List<CompanionPushDelivery> sendableDeliveries = new ArrayList<>();
        List<PushMessage> messages = new ArrayList<>();
        for (CompanionPushDelivery delivery : deliveries) {
            CompanionNotification notification = notifications.get(delivery.notificationId());
            if (notification == null) {
                markPermanentFailure(delivery, "NOTIFICATION_NOT_FOUND", claimToken, now);
                continue;
            }
            sendableDeliveries.add(delivery);
            messages.add(toMessage(delivery, notification));
        }
        if (messages.isEmpty()) {
            return;
        }

        List<PushDeliveryResult> providerResults = sender.send(messages);
        Map<Long, PushDeliveryResult> results = index(providerResults == null ? List.of() : providerResults);
        for (CompanionPushDelivery delivery : sendableDeliveries) {
            PushDeliveryResult result = results.get(delivery.id());
            if (result == null) {
                retry(delivery, claimToken, "MISSING_PROVIDER_RESULT", null, now);
                continue;
            }
            applyResult(delivery, result, claimToken, now);
        }
    }

    private PushMessage toMessage(
            final CompanionPushDelivery delivery,
            final CompanionNotification notification
    ) {
        return new PushMessage(
                delivery.id(),
                delivery.token(),
                delivery.title(),
                delivery.body(),
                Map.of(
                        "type", notification.notificationType().name(),
                        "applicationId", String.valueOf(delivery.targetId()),
                        "notificationId", String.valueOf(delivery.notificationId()),
                        "targetType", delivery.targetType().name(),
                        "targetId", String.valueOf(delivery.targetId())
                ),
                delivery.expiresAt()
        );
    }

    private Map<Long, PushDeliveryResult> index(final List<PushDeliveryResult> results) {
        Map<Long, PushDeliveryResult> indexed = new HashMap<>();
        for (PushDeliveryResult result : results) {
            if (result != null && result.deliveryId() != null) {
                indexed.put(result.deliveryId(), result);
            }
        }
        return indexed;
    }

    private void applyResult(
            final CompanionPushDelivery delivery,
            final PushDeliveryResult result,
            final String claimToken,
            final LocalDateTime now
    ) {
        switch (result.outcome()) {
            case SENT -> repository.markSent(delivery.id(), claimToken, result.providerMessageId(), now);
            case PERMANENT_FAILURE -> markPermanentFailure(delivery, result.errorCode(), claimToken, now);
            case RETRYABLE_FAILURE -> retry(
                    delivery,
                    claimToken,
                    result.errorCode(),
                    result.retryAfterSeconds(),
                    now
            );
        }
    }

    private void markPermanentFailure(
            final CompanionPushDelivery delivery,
            final String errorCode,
            final String claimToken,
            final LocalDateTime now
    ) {
        boolean marked = repository.markPermanentFailure(delivery.id(), claimToken, errorCode, now);
        if (marked && endpointRepository != null && "UNREGISTERED".equals(errorCode)) {
            endpointRepository.deactivateIfCurrent(
                    delivery.endpointId(),
                    delivery.endpointRegistrationVersion(),
                    delivery.token(),
                    now
            );
        }
    }

    private void retry(
            final CompanionPushDelivery delivery,
            final String claimToken,
            final String errorCode,
            final Long retryAfterSeconds,
            final LocalDateTime now
    ) {
        if (delivery.attemptCount() >= maxAttempts) {
            markPermanentFailure(delivery, errorCode, claimToken, now);
            return;
        }
        long exponentialDelaySeconds = Math.min(3600L, 1L << Math.min(delivery.attemptCount(), 11));
        long providerDelaySeconds = retryAfterSeconds == null ? 0L : Math.max(0L, retryAfterSeconds);
        long minimumDelaySeconds = "QUOTA_EXCEEDED".equals(errorCode)
                ? 60L
                : 0L;
        long jitterSeconds = Math.max(0, jitterSecondsSupplier.getAsInt());
        long baseDelaySeconds = Math.min(
                86_400L,
                Math.max(exponentialDelaySeconds, Math.max(providerDelaySeconds, minimumDelaySeconds))
        );
        long remainingDelaySeconds = 86_400L - baseDelaySeconds;
        long delaySeconds = baseDelaySeconds + Math.min(jitterSeconds, remainingDelaySeconds);
        repository.markRetry(
                delivery.id(),
                claimToken,
                now.plusSeconds(delaySeconds),
                errorCode,
                now
        );
    }

}
