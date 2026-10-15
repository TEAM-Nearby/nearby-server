// 동행 푸시 수신 대상 등록 결과를 표현하는 DTO다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.RegisterCompanionPushEndpointResult;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;

public record CompanionPushEndpointResponse(
        Long endpointId,
        String installationId,
        CompanionPushPlatform platform,
        boolean active
) {

    public static CompanionPushEndpointResponse from(final RegisterCompanionPushEndpointResult result) {
        return new CompanionPushEndpointResponse(
                result.endpointId(),
                result.installationId(),
                result.platform(),
                result.active()
        );
    }
}
