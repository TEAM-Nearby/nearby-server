// 만남 인증에 필요한 조회 데이터를 가져오는 포트
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingCheckInContext;
import java.util.Optional;

public interface CompanionMeetingCheckInQueryPort {

    Optional<CompanionMeetingCheckInContext> findByMeetingId(Long meetingId);
}
