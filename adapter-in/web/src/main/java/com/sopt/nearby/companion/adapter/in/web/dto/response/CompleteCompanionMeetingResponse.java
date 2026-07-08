// 동행 마치기 결과를 HTTP 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompleteCompanionMeetingResult;
import java.time.LocalDateTime;

public record CompleteCompanionMeetingResponse(
		Long meetingId,
		Long matchId,
		String meetingStatus,
		LocalDateTime completedAt
) {

	public static CompleteCompanionMeetingResponse from(final CompleteCompanionMeetingResult result) {
		return new CompleteCompanionMeetingResponse(
				result.meetingId(),
				result.matchId(),
				result.meetingStatus().name(),
				result.completedAt()
		);
	}
}
