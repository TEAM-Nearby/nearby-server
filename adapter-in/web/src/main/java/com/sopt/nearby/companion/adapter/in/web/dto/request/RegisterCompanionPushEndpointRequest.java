// 동행 푸시 수신 대상 등록 요청을 유스케이스 명령으로 변환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.RegisterCompanionPushEndpointCommand;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPushEndpointException;
import com.sopt.nearby.companion.domain.model.notification.CompanionPushPlatform;
import java.util.Locale;

public record RegisterCompanionPushEndpointRequest(
        String installationId,
        String token,
        String platform
) {

    public RegisterCompanionPushEndpointCommand toCommand(final Long userId) {
        try {
            return new RegisterCompanionPushEndpointCommand(
                    userId,
                    installationId,
                    token,
                    CompanionPushPlatform.valueOf(platform.toUpperCase(Locale.ROOT))
            );
        } catch (RuntimeException exception) {
            throw new InvalidCompanionPushEndpointException();
        }
    }
}
