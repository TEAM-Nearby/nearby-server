// 동행 신청자가 처리 결과를 조회하는 유스케이스 진입점
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompanionRequestResult;
import com.sopt.nearby.companion.application.ReadCompanionRequestResultCommand;

public interface ReadCompanionRequestResultUseCase {

    CompanionRequestResult read(ReadCompanionRequestResultCommand command);
}
