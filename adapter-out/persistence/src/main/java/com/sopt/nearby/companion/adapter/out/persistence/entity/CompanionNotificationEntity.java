// 동행 알림 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationTargetType;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationType;
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
        name = "companion_notification",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_companion_notification_event_recipient",
                columnNames = {"notification_type", "target_type", "target_id", "recipient_user_id"}
        )
)
public class CompanionNotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private CompanionNotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private CompanionNotificationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CompanionNotificationEntity() {
    }

    public CompanionNotificationEntity(
            final Long id,
            final Long recipientUserId,
            final CompanionNotificationType notificationType,
            final CompanionNotificationTargetType targetType,
            final Long targetId,
            final LocalDateTime readAt,
            final LocalDateTime createdAt
    ) {
        this.id = id;
        this.recipientUserId = recipientUserId;
        this.notificationType = notificationType;
        this.targetType = targetType;
        this.targetId = targetId;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getRecipientUserId() {
        return recipientUserId;
    }

    public CompanionNotificationType getNotificationType() {
        return notificationType;
    }

    public CompanionNotificationTargetType getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void markAsRead(final LocalDateTime readAt) {
        if (this.readAt == null) {
            this.readAt = readAt;
        }
    }
}
