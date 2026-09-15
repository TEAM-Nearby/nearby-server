// 동행 푸시 발송 작업을 배치로 확보하고 결과를 반영하는 서비스다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.port.in.ProcessCompanionPushDeliveriesUseCase;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import com.sopt.nearby.companion.port.out.CompanionPushEndpointRepository;
import com.sopt.nearby.companion.port.out.PushDeliveryResult;
import com.sopt.nearby.companion.port.out.PushMessage;
import com.sopt.nearby.companion.port.out.PushSender;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProcessCompanionPushDeliveriesService implements ProcessCompanionPushDeliveriesUseCase {

    private final CompanionPushDeliveryRepository repository;
    private final CompanionPushEndpointRepository endpointRepository;
    private final PushSender sender;
    private final Clock clock;
    private final int batchSize;
    private final int maxAttempts;
    private final Duration leaseDuration;

    public ProcessCompanionPushDeliveriesService(
            final CompanionPushDeliveryRepository repository,
            final PushSender sender,
            final Clock clock,
            final int batchSize,
            final int maxAttempts,
            final Duration leaseDuration
    ) {
        this(repository, null, sender, clock, batchSize, maxAttempts, leaseDuration);
    }

    public ProcessCompanionPushDeliveriesService(
            final CompanionPushDeliveryRepository repository,
            final CompanionPushEndpointRepository endpointRepository,
            final PushSender sender,
            final Clock clock,
            final int batchSize,
            final int maxAttempts,
            final Duration leaseDuration
    ) {
        this.repository = repository;
        this.endpointRepository = endpointRepository;
        this.sender = sender;
        this.clock = clock;
        this.batchSize = Math.min(Math.max(batchSize, 1), 500);
        this.maxAttempts = Math.max(maxAttempts, 1);
        this.leaseDuration = leaseDuration.isNegative() || leaseDuration.isZero()
                ? Duration.ofSeconds(30)
                : leaseDuration;
    }

    @Override
    public void processBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        repository.skipInactiveEndpointDeliveries(now);
        repository.expireExpiredDeliveries(now);
        repository.recoverExpiredLeases(now);

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

        Map<Long, PushDeliveryResult> results = index(sender.send(deliveries.stream()
                .map(this::toMessage)
                .toList()));
        for (CompanionPushDelivery delivery : deliveries) {
            PushDeliveryResult result = results.get(delivery.id());
            if (result == null) {
                retry(delivery, claimToken, "MISSING_PROVIDER_RESULT", now);
                continue;
            }
            applyResult(delivery, result, claimToken, now);
        }
    }

    private PushMessage toMessage(final CompanionPushDelivery delivery) {
        return new PushMessage(
                delivery.id(),
                delivery.token(),
                delivery.title(),
                delivery.body(),
                Map.of(
                        "notificationId", String.valueOf(delivery.notificationId()),
                        "targetType", delivery.targetType().name(),
                        "targetId", String.valueOf(delivery.targetId())
                )
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
            case RETRYABLE_FAILURE -> retry(delivery, claimToken, result.errorCode(), now);
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
            endpointRepository.deactivateById(delivery.endpointId(), now);
        }
    }

    private void retry(
            final CompanionPushDelivery delivery,
            final String claimToken,
            final String errorCode,
            final LocalDateTime now
    ) {
        if (delivery.attemptCount() >= maxAttempts) {
            markPermanentFailure(delivery, errorCode, claimToken, now);
            return;
        }
        long delaySeconds = Math.min(3600L, 1L << Math.min(delivery.attemptCount(), 11));
        repository.markRetry(
                delivery.id(),
                claimToken,
                now.plusSeconds(delaySeconds),
                errorCode,
                now
        );
    }
}
