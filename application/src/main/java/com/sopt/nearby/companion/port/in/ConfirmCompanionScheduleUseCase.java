// 동행 일정 수정 유스케이스의 진입 포트를 정의
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ConfirmCompanionScheduleCommand;
import com.sopt.nearby.companion.application.ConfirmCompanionScheduleResult;

public interface ConfirmCompanionScheduleUseCase {
    ConfirmCompanionScheduleResult update(ConfirmCompanionScheduleCommand command);
}
