// 동행 푸시 발송 작업용 JPA 저장소다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPushDeliveryEntity;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDeliveryStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CompanionPushDeliveryJpaRepository extends JpaRepository<CompanionPushDeliveryEntity, Long> {

    Optional<CompanionPushDeliveryEntity> findByNotificationIdAndEndpointId(
            Long notificationId,
            Long endpointId
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            insert into companion_push_delivery (
                notification_id, endpoint_id, recipient_user_id, token, title, body,
                target_type, target_id, status, attempt_count, next_attempt_at,
                lease_until, claim_token, provider_message_id, last_error_code,
                expires_at, created_at, updated_at
            ) values (
                :notificationId, :endpointId, :recipientUserId, :token, :title, :body,
                :targetType, :targetId, :status, :attemptCount, :nextAttemptAt,
                :leaseUntil, :claimToken, :providerMessageId, :lastErrorCode,
                :expiresAt, :createdAt, :updatedAt
            )
            on conflict (notification_id, endpoint_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("notificationId") Long notificationId,
            @Param("endpointId") Long endpointId,
            @Param("recipientUserId") Long recipientUserId,
            @Param("token") String token,
            @Param("title") String title,
            @Param("body") String body,
            @Param("targetType") String targetType,
            @Param("targetId") Long targetId,
            @Param("status") String status,
            @Param("attemptCount") int attemptCount,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("leaseUntil") LocalDateTime leaseUntil,
            @Param("claimToken") String claimToken,
            @Param("providerMessageId") String providerMessageId,
            @Param("lastErrorCode") String lastErrorCode,
            @Param("expiresAt") LocalDateTime expiresAt,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    @Query(value = """
            select delivery.*
            from companion_push_delivery delivery
            join companion_push_endpoint endpoint on endpoint.id = delivery.endpoint_id
            where delivery.status in ('PENDING', 'RETRY')
                and delivery.next_attempt_at <= :now
                and delivery.expires_at > :now
                and endpoint.active = true
                and endpoint.user_id = delivery.recipient_user_id
            order by delivery.next_attempt_at asc, delivery.id asc
            limit :batchSize
            for update of delivery skip locked
            """, nativeQuery = true)
    List<CompanionPushDeliveryEntity> findDueForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :expiredStatus,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.lastErrorCode = :errorCode,
                delivery.updatedAt = :now
            where delivery.status in (:pendingStatus, :retryStatus, :processingStatus)
                and delivery.expiresAt <= :now
            """)
    int expireExpiredDeliveries(
            @Param("now") LocalDateTime now,
            @Param("expiredStatus") CompanionPushDeliveryStatus expiredStatus,
            @Param("pendingStatus") CompanionPushDeliveryStatus pendingStatus,
            @Param("retryStatus") CompanionPushDeliveryStatus retryStatus,
            @Param("processingStatus") CompanionPushDeliveryStatus processingStatus,
            @Param("errorCode") String errorCode
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :skippedStatus,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.lastErrorCode = :errorCode,
                delivery.updatedAt = :now
            where delivery.status in (:pendingStatus, :retryStatus)
                and exists (
                    select 1
                    from CompanionPushEndpointEntity endpoint
                    where endpoint.id = delivery.endpointId
                        and (endpoint.active = false or endpoint.userId <> delivery.recipientUserId)
                )
            """)
    int skipInactiveEndpointDeliveries(
            @Param("now") LocalDateTime now,
            @Param("skippedStatus") CompanionPushDeliveryStatus skippedStatus,
            @Param("pendingStatus") CompanionPushDeliveryStatus pendingStatus,
            @Param("retryStatus") CompanionPushDeliveryStatus retryStatus,
            @Param("errorCode") String errorCode
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :status,
                delivery.nextAttemptAt = :now,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.updatedAt = :now,
                delivery.lastErrorCode = :errorCode
            where delivery.status = :processingStatus
                and delivery.leaseUntil is not null
                and delivery.leaseUntil <= :now
                and delivery.expiresAt > :now
            """)
    int recoverExpiredLeases(
            @Param("now") LocalDateTime now,
            @Param("status") CompanionPushDeliveryStatus status,
            @Param("processingStatus") CompanionPushDeliveryStatus processingStatus,
            @Param("errorCode") String errorCode
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :status,
                delivery.providerMessageId = :providerMessageId,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.updatedAt = :now
            where delivery.id = :deliveryId
                and delivery.status = :processingStatus
                and delivery.claimToken = :claimToken
            """)
    int markSent(
            @Param("deliveryId") Long deliveryId,
            @Param("claimToken") String claimToken,
            @Param("providerMessageId") String providerMessageId,
            @Param("now") LocalDateTime now,
            @Param("status") CompanionPushDeliveryStatus status,
            @Param("processingStatus") CompanionPushDeliveryStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :status,
                delivery.nextAttemptAt = :nextAttemptAt,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.lastErrorCode = :errorCode,
                delivery.updatedAt = :now
            where delivery.id = :deliveryId
                and delivery.status = :processingStatus
                and delivery.claimToken = :claimToken
            """)
    int markRetry(
            @Param("deliveryId") Long deliveryId,
            @Param("claimToken") String claimToken,
            @Param("nextAttemptAt") LocalDateTime nextAttemptAt,
            @Param("errorCode") String errorCode,
            @Param("now") LocalDateTime now,
            @Param("status") CompanionPushDeliveryStatus status,
            @Param("processingStatus") CompanionPushDeliveryStatus processingStatus
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update CompanionPushDeliveryEntity delivery
            set delivery.status = :status,
                delivery.leaseUntil = null,
                delivery.claimToken = null,
                delivery.lastErrorCode = :errorCode,
                delivery.updatedAt = :now
            where delivery.id = :deliveryId
                and delivery.status = :processingStatus
                and delivery.claimToken = :claimToken
            """)
    int markTerminal(
            @Param("deliveryId") Long deliveryId,
            @Param("claimToken") String claimToken,
            @Param("errorCode") String errorCode,
            @Param("now") LocalDateTime now,
            @Param("status") CompanionPushDeliveryStatus status,
            @Param("processingStatus") CompanionPushDeliveryStatus processingStatus
    );
}
