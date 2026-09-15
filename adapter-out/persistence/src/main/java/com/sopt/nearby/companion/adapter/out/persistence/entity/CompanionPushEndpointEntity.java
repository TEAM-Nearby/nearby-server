// 동행 푸시 수신 대상 테이블을 매핑하는 JPA 엔티티다.
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
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
        name = "companion_push_endpoint",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_companion_push_endpoint_user_installation",
                columnNames = {"user_id", "installation_id"}
        )
)
public class CompanionPushEndpointEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "installation_id", nullable = false, length = 200)
    private String installationId;

    @Column(nullable = false, length = 4096)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionPushPlatform platform;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "registration_version", nullable = false)
    private long registrationVersion;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected CompanionPushEndpointEntity() {
    }

    public CompanionPushEndpointEntity(
            final Long id,
            final Long userId,
            final String installationId,
            final String token,
            final CompanionPushPlatform platform,
            final boolean active,
            final long registrationVersion,
            final LocalDateTime lastSeenAt,
            final LocalDateTime createdAt,
            final LocalDateTime updatedAt
    ) {
        this.id = id;
        this.userId = userId;
        this.installationId = installationId;
        this.token = token;
        this.platform = platform;
        this.active = active;
        this.registrationVersion = registrationVersion;
        this.lastSeenAt = lastSeenAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getInstallationId() {
        return installationId;
    }

    public String getToken() {
        return token;
    }

    public CompanionPushPlatform getPlatform() {
        return platform;
    }

    public boolean isActive() {
        return active;
    }

    public long getRegistrationVersion() {
        return registrationVersion;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
