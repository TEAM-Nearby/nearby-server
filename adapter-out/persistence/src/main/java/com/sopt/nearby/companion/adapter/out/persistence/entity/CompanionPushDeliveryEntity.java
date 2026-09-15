// 동행 푸시 발송 작업 테이블을 매핑하는 JPA 엔티티다.
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "companion_push_delivery",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_companion_push_delivery_notification_endpoint",
                columnNames = {"notification_id", "endpoint_id"}
        )
)
public class CompanionPushDeliveryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "notification_id", nullable = false)
    private Long notificationId;

    @Column(name = "endpoint_id", nullable = false)
    private Long endpointId;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(nullable = false, length = 4096)
    private String token;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private CompanionNotificationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CompanionPushDeliveryStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "lease_until")
    private LocalDateTime leaseUntil;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "provider_message_id", length = 255)
    private String providerMessageId;

    @Column(name = "last_error_code", length = 255)
    private String lastErrorCode;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CompanionPushDeliveryEntity() {
    }

    public CompanionPushDeliveryEntity(
            final Long id,
            final Long notificationId,
            final Long endpointId,
            final Long recipientUserId,
            final String token,
            final String title,
            final String body,
            final CompanionNotificationTargetType targetType,
            final Long targetId,
            final CompanionPushDeliveryStatus status,
            final int attemptCount,
            final LocalDateTime nextAttemptAt,
            final LocalDateTime leaseUntil,
            final String claimToken,
            final String providerMessageId,
            final String lastErrorCode,
            final LocalDateTime expiresAt,
            final LocalDateTime createdAt,
            final LocalDateTime updatedAt
    ) {
        this.id = id;
        this.notificationId = notificationId;
        this.endpointId = endpointId;
        this.recipientUserId = recipientUserId;
        this.token = token;
        this.title = title;
        this.body = body;
        this.targetType = targetType;
        this.targetId = targetId;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.leaseUntil = leaseUntil;
        this.claimToken = claimToken;
        this.providerMessageId = providerMessageId;
        this.lastErrorCode = lastErrorCode;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public Long getNotificationId() { return notificationId; }
    public Long getEndpointId() { return endpointId; }
    public Long getRecipientUserId() { return recipientUserId; }
    public String getToken() { return token; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public CompanionNotificationTargetType getTargetType() { return targetType; }
    public Long getTargetId() { return targetId; }
    public CompanionPushDeliveryStatus getStatus() { return status; }
    public int getAttemptCount() { return attemptCount; }
    public LocalDateTime getNextAttemptAt() { return nextAttemptAt; }
    public LocalDateTime getLeaseUntil() { return leaseUntil; }
    public String getClaimToken() { return claimToken; }
    public String getProviderMessageId() { return providerMessageId; }
    public String getLastErrorCode() { return lastErrorCode; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void claim(final LocalDateTime now, final LocalDateTime leaseUntil, final String claimToken) {
        this.status = CompanionPushDeliveryStatus.PROCESSING;
        this.attemptCount++;
        this.leaseUntil = leaseUntil;
        this.claimToken = claimToken;
        this.updatedAt = now;
    }
}
