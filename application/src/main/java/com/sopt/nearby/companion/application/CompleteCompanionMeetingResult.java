// 동행 마치기 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import java.time.LocalDateTime;

public record CompleteCompanionMeetingResult(
		Long meetingId,
		Long matchId,
		boolean currentUserCompleted,
		LocalDateTime currentUserCompletedAt,
		CompanionMeetingStatus meetingStatus,
		LocalDateTime meetingCompletedAt
) {
}
