// 사용자의 앱 설치별 푸시 수신 대상을 표현한다.
package com.sopt.nearby.companion.domain.model.notification;

import java.time.LocalDateTime;

public record CompanionPushEndpoint(
        Long id,
        Long userId,
        String installationId,
        String token,
        CompanionPushPlatform platform,
        boolean active,
        long registrationVersion,
        LocalDateTime lastSeenAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public CompanionPushEndpoint activate(
            final String token,
            final CompanionPushPlatform platform,
            final LocalDateTime now
    ) {
        return new CompanionPushEndpoint(
                id,
                userId,
                installationId,
                token,
                platform,
                true,
                token.equals(this.token) && platform == this.platform
                        ? registrationVersion
                        : registrationVersion + 1,
                now,
                createdAt,
                now
        );
    }

    public CompanionPushEndpoint deactivate(final LocalDateTime now) {
        return new CompanionPushEndpoint(
                id,
                userId,
                installationId,
                token,
                platform,
                false,
                registrationVersion,
                lastSeenAt,
                createdAt,
                now
        );
    }
}
