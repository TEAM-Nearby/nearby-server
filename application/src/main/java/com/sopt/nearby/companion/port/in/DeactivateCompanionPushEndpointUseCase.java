// 동행 푸시 수신 대상을 비활성화하는 인바운드 포트다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.DeactivateCompanionPushEndpointCommand;

public interface DeactivateCompanionPushEndpointUseCase {

    void deactivate(DeactivateCompanionPushEndpointCommand command);
}
