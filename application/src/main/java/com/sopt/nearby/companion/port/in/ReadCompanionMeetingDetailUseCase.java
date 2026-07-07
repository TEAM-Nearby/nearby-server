// 진행 중인 동행 상세 조회 유스케이스 진입점을 정의하는 포트
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.application.ReadCompanionMeetingDetailResult;

public interface ReadCompanionMeetingDetailUseCase {

    ReadCompanionMeetingDetailResult getDetail(Long meetingId, Long userId);
}
