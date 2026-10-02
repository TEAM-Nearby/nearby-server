// 인증된 사용자 본인의 프로필 수정을 제공한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.MyCompanionProfileResult;
import com.sopt.nearby.companion.application.UpdateMyCompanionProfileCommand;

public interface UpdateMyCompanionProfileUseCase {
    MyCompanionProfileResult update(UpdateMyCompanionProfileCommand command);
}
