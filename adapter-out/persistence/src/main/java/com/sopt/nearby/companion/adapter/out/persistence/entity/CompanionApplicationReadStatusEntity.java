// 동행 알림 읽음 상태 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "companion_application_read_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_companion_application_read_status_application_user",
                columnNames = {"application_id", "user_id"}
        )
)
public class CompanionApplicationReadStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", insertable = false, updatable = false)
    private CompanionApplicationEntity application;

    protected CompanionApplicationReadStatusEntity() {
    }

    public CompanionApplicationReadStatusEntity(
            final Long id,
            final Long applicationId,
            final Long userId,
            final LocalDateTime readAt
    ) {
        this.id = id;
        this.applicationId = applicationId;
        this.userId = userId;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
