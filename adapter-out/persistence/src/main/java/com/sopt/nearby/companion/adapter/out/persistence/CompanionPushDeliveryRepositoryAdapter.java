// 동행 푸시 발송 작업 포트를 JPA로 구현하는 어댑터다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPushDeliveryEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPushDeliveryJpaRepository;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDelivery;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDeliveryStatus;
import com.sopt.nearby.companion.port.out.CompanionPushDeliveryRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class CompanionPushDeliveryRepositoryAdapter implements CompanionPushDeliveryRepository {

    private final CompanionPushDeliveryJpaRepository repository;

    public CompanionPushDeliveryRepositoryAdapter(final CompanionPushDeliveryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public CompanionPushDelivery saveIfAbsent(final CompanionPushDelivery delivery) {
        repository.insertIfAbsent(
                delivery.notificationId(),
                delivery.endpointId(),
                delivery.recipientUserId(),
                delivery.token(),
                delivery.title(),
                delivery.body(),
                delivery.targetType().name(),
                delivery.targetId(),
                delivery.status().name(),
                delivery.attemptCount(),
                delivery.nextAttemptAt(),
                delivery.leaseUntil(),
                delivery.claimToken(),
                delivery.providerMessageId(),
                delivery.lastErrorCode(),
                delivery.expiresAt(),
                delivery.createdAt(),
                delivery.updatedAt()
        );
        return repository.findByNotificationIdAndEndpointId(
                        delivery.notificationId(),
                        delivery.endpointId()
                )
                .map(CompanionPersistenceMapper::toDomain)
                .orElseThrow(() -> new IllegalStateException("푸시 발송 작업을 저장할 수 없습니다."));
    }

    @Override
    @Transactional
    public List<CompanionPushDelivery> claimDue(
            final int batchSize,
            final LocalDateTime now,
            final LocalDateTime leaseUntil,
            final String claimToken
    ) {
        List<CompanionPushDeliveryEntity> entities = repository.findDueForUpdate(now, batchSize);
        entities.forEach(entity -> entity.claim(now, leaseUntil, claimToken));
        return entities.stream().map(CompanionPersistenceMapper::toDomain).toList();
    }

    @Override
    @Transactional
    public boolean markSent(
            final Long deliveryId,
            final String claimToken,
            final String providerMessageId,
            final LocalDateTime now
    ) {
        return repository.markSent(
                deliveryId,
                claimToken,
                providerMessageId,
                now,
                CompanionPushDeliveryStatus.SENT,
                CompanionPushDeliveryStatus.PROCESSING
        ) == 1;
    }

    @Override
    @Transactional
    public boolean markRetry(
            final Long deliveryId,
            final String claimToken,
            final LocalDateTime nextAttemptAt,
            final String errorCode,
            final LocalDateTime now
    ) {
        return repository.markRetry(
                deliveryId,
                claimToken,
                nextAttemptAt,
                errorCode,
                now,
                CompanionPushDeliveryStatus.RETRY,
                CompanionPushDeliveryStatus.PROCESSING
        ) == 1;
    }

    @Override
    @Transactional
    public boolean markPermanentFailure(
            final Long deliveryId,
            final String claimToken,
            final String errorCode,
            final LocalDateTime now
    ) {
        return repository.markTerminal(
                deliveryId,
                claimToken,
                errorCode,
                now,
                CompanionPushDeliveryStatus.FAILED_PERMANENT,
                CompanionPushDeliveryStatus.PROCESSING
        ) == 1;
    }

    @Override
    @Transactional
    public boolean markExpired(
            final Long deliveryId,
            final String claimToken,
            final String errorCode,
            final LocalDateTime now
    ) {
        return repository.markTerminal(
                deliveryId,
                claimToken,
                errorCode,
                now,
                CompanionPushDeliveryStatus.EXPIRED,
                CompanionPushDeliveryStatus.PROCESSING
        ) == 1;
    }

    @Override
    @Transactional
    public int expireExpiredDeliveries(final LocalDateTime now) {
        return repository.expireExpiredDeliveries(
                now,
                CompanionPushDeliveryStatus.EXPIRED,
                CompanionPushDeliveryStatus.PENDING,
                CompanionPushDeliveryStatus.RETRY,
                CompanionPushDeliveryStatus.PROCESSING,
                "DELIVERY_EXPIRED"
        );
    }

    @Override
    @Transactional
    public int skipInactiveEndpointDeliveries(final LocalDateTime now) {
        return repository.skipInactiveEndpointDeliveries(
                now,
                CompanionPushDeliveryStatus.SKIPPED,
                CompanionPushDeliveryStatus.PENDING,
                CompanionPushDeliveryStatus.RETRY,
                "ENDPOINT_INACTIVE"
        );
    }

    @Override
    @Transactional
    public int recoverExpiredLeases(final LocalDateTime now) {
        return repository.recoverExpiredLeases(
                now,
                CompanionPushDeliveryStatus.RETRY,
                CompanionPushDeliveryStatus.PROCESSING,
                "LEASE_EXPIRED"
        );
    }
}
