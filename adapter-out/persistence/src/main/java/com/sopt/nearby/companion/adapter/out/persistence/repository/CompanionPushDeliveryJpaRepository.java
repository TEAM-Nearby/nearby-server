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
                notification_id, endpoint_id, endpoint_registration_version, recipient_user_id, token, title, body,
                target_type, target_id, status, attempt_count, next_attempt_at,
                lease_until, claim_token, provider_message_id, last_error_code,
                expires_at, created_at, updated_at
            ) values (
                :notificationId, :endpointId, :endpointRegistrationVersion, :recipientUserId, :token, :title, :body,
                :targetType, :targetId, :status, :attemptCount, :nextAttemptAt,
                :leaseUntil, :claimToken, :providerMessageId, :lastErrorCode,
                :expiresAt, :createdAt, :updatedAt
            )
            on conflict (notification_id, endpoint_id) do nothing
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("notificationId") Long notificationId,
            @Param("endpointId") Long endpointId,
            @Param("endpointRegistrationVersion") long endpointRegistrationVersion,
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
                and endpoint.registration_version = delivery.endpoint_registration_version
                and endpoint.token = delivery.token
            order by delivery.next_attempt_at asc, delivery.id asc
            limit :batchSize
            for update of delivery skip locked
            """, nativeQuery = true)
    List<CompanionPushDeliveryEntity> findDueForUpdate(
            @Param("now") LocalDateTime now,
            @Param("batchSize") int batchSize
    );

    @Modifying
    @Query(value = """
            with candidates as (
                select id
                from companion_push_delivery
                where status in ('PENDING', 'RETRY', 'PROCESSING')
                    and expires_at <= :now
                order by id
                limit :limit
                for update skip locked
            )
            update companion_push_delivery delivery
            set status = 'EXPIRED', lease_until = null, claim_token = null,
                last_error_code = 'DELIVERY_EXPIRED', updated_at = :now
            where delivery.id in (select id from candidates)
            """, nativeQuery = true)
    int expireExpiredDeliveries(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Query(value = """
            with candidates as (
                select delivery.id
                from companion_push_delivery delivery
                join companion_push_endpoint endpoint on endpoint.id = delivery.endpoint_id
                where delivery.status in ('PENDING', 'RETRY')
                    and (
                        endpoint.active = false
                        or endpoint.user_id <> delivery.recipient_user_id
                        or endpoint.registration_version <> delivery.endpoint_registration_version
                        or endpoint.token <> delivery.token
                    )
                order by delivery.id
                limit :limit
                for update of delivery skip locked
            )
            update companion_push_delivery delivery
            set status = 'SKIPPED', lease_until = null, claim_token = null,
                last_error_code = 'ENDPOINT_INACTIVE', updated_at = :now
            where delivery.id in (select id from candidates)
            """, nativeQuery = true)
    int skipInactiveEndpointDeliveries(@Param("now") LocalDateTime now, @Param("limit") int limit);

    @Modifying
    @Query(value = """
            with candidates as (
                select id, attempt_count
                from companion_push_delivery
                where status = 'PROCESSING'
                    and lease_until is not null
                    and lease_until <= :now
                    and expires_at > :now
                order by id
                limit :limit
                for update skip locked
            )
            update companion_push_delivery delivery
            set status = case when candidates.attempt_count >= :maxAttempts
                              then 'FAILED_PERMANENT' else 'RETRY' end,
                next_attempt_at = :now, lease_until = null, claim_token = null,
                last_error_code = case when candidates.attempt_count >= :maxAttempts
                                      then 'LEASE_EXPIRED_MAX_ATTEMPTS' else 'LEASE_EXPIRED' end,
                updated_at = :now
            from candidates
            where delivery.id = candidates.id
            """, nativeQuery = true)
    int recoverExpiredLeases(
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts,
            @Param("limit") int limit
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
