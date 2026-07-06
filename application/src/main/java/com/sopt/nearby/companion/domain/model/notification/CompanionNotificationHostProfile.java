// 동행 알림 목록에서 반환할 HOST 프로필 정보를 표현하는 모델
package com.sopt.nearby.companion.domain.model.notification;

public record CompanionNotificationHostProfile(
        Long userId,
        String profileImageUrl,
        String nickname
) {
}

