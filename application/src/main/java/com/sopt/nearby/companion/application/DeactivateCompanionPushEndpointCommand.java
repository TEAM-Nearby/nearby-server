// 동행 푸시 수신 대상 비활성화에 필요한 값을 전달한다.
package com.sopt.nearby.companion.application;

public record DeactivateCompanionPushEndpointCommand(
        Long userId,
        String installationId
) {
}
