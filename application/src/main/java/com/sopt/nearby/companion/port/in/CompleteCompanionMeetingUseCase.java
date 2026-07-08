// 동행 마치기 유스케이스 진입점을 정의하는 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.CompleteCompanionMeetingResult;

public interface CompleteCompanionMeetingUseCase {

	CompleteCompanionMeetingResult complete(Long meetingId, Long userId);
}
