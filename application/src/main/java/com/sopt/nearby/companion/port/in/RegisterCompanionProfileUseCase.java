// 동행 프로필 등록 유스케이스 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.RegisterCompanionProfileCommand;
import com.sopt.nearby.companion.application.RegisteredCompanionProfileResult;

public interface RegisterCompanionProfileUseCase {

	RegisteredCompanionProfileResult register(RegisterCompanionProfileCommand command);
}

