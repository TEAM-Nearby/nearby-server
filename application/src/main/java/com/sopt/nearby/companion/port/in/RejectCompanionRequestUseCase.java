// 동행 신청 거절 유스케이스의 진입 포트를 정의한다.
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.RejectCompanionRequestCommand;
import com.sopt.nearby.companion.application.RejectedCompanionRequestResult;

public interface RejectCompanionRequestUseCase {

    RejectedCompanionRequestResult reject(RejectCompanionRequestCommand command);
}
