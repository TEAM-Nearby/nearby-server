// 진행 중인 동행 상세 조회 데이터를 가져오는 포트
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import java.util.Optional;

public interface CompanionMeetingDetailQueryPort {

    Optional<CompanionMeetingDetail> findByMeetingIdAndUserId(Long meetingId, Long userId);
}
