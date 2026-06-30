// 동행 미팅 취소 이력을 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

import java.time.LocalDateTime;

public record MeetingCancellation(
		Long id,
		Long meetingId,
		Long canceledByUserId,
		String reason,
		LocalDateTime canceledAt
) {
}
