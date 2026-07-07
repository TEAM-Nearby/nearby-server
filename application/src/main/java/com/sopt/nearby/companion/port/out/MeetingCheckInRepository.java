// 미팅 체크인 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCheckIn;
import java.util.Optional;

public interface MeetingCheckInRepository extends DomainRepository<MeetingCheckIn, Long> {

    Optional<MeetingCheckIn> findByMeetingIdAndUserId(Long meetingId, Long userId);

    long countByMeetingId(Long meetingId);

    MeetingCheckIn saveIfAbsent(MeetingCheckIn checkIn);
}
