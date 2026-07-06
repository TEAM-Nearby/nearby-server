// 동행 알림 목록에서 반환할 HOST 프로필 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationHostProfile;

public record CompanionNotificationHostResponse(
        Long userId,
        String profileImageUrl,
        String nickname
) {

    public static CompanionNotificationHostResponse from(final CompanionNotificationHostProfile host) {
        return new CompanionNotificationHostResponse(
                host.userId(),
                host.profileImageUrl(),
                host.nickname()
        );
    }
}

