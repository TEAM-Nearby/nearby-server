// 동행 신청 수락 유스케이스의 진입 포트를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.AcceptCompanionRequestCommand;
import com.sopt.nearby.companion.application.AcceptedCompanionRequestResult;

public interface AcceptCompanionRequestUseCase {

    AcceptedCompanionRequestResult accept(AcceptCompanionRequestCommand command);
}
