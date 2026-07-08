// 동행 미팅 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import java.time.LocalDateTime;

public interface CompanionMeetingRepository extends DomainRepository<CompanionMeeting, Long> {

	boolean completeIfOngoing(Long meetingId, LocalDateTime completedAt);
}
