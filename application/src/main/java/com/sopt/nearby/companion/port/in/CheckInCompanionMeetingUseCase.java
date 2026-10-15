// 동행 만남 인증 유스케이스 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CheckInCompanionMeetingCommand;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;

public interface CheckInCompanionMeetingUseCase {

    CheckInCompanionMeetingResult checkIn(CheckInCompanionMeetingCommand command);
}
