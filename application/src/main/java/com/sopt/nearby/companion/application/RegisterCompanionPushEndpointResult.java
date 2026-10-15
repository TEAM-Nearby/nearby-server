// 동행 푸시 수신 대상 등록 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionPushEndpoint;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;

public record RegisterCompanionPushEndpointResult(
        Long endpointId,
        String installationId,
        CompanionPushPlatform platform,
        boolean active
) {

    public static RegisterCompanionPushEndpointResult from(final CompanionPushEndpoint endpoint) {
        return new RegisterCompanionPushEndpointResult(
                endpoint.id(),
                endpoint.installationId(),
                endpoint.platform(),
                endpoint.active()
        );
    }
}
