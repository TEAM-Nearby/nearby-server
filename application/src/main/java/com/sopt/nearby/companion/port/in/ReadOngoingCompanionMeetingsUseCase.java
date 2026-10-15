// 진행 중인 동행 목록 조회 유스케이스 진입점을 정의하는 인터페이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import java.util.List;

public interface ReadOngoingCompanionMeetingsUseCase {

    List<OngoingCompanionMeetingSummary> getOngoingMeetings(Long userId);
}
