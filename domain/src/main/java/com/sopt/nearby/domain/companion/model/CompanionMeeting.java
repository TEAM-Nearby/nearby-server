// 동행 미팅 정보를 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

import java.time.LocalDateTime;

public record CompanionMeeting(
		Long id,
		Long matchId,
		CompanionMeetingStatus status,
		LocalDateTime startedAt,
		LocalDateTime completedAt
) {
}
