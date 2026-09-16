// 동행 푸시 수신 대상을 등록·갱신하는 인바운드 포트다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.RegisterCompanionPushEndpointCommand;
import com.sopt.nearby.companion.application.RegisterCompanionPushEndpointResult;

public interface RegisterCompanionPushEndpointUseCase {

    RegisterCompanionPushEndpointResult register(RegisterCompanionPushEndpointCommand command);
}
