// 동행 푸시 수신 대상 등록에 필요한 값을 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;

public record RegisterCompanionPushEndpointCommand(
        Long userId,
        String installationId,
        String token,
        CompanionPushPlatform platform
) {
}
