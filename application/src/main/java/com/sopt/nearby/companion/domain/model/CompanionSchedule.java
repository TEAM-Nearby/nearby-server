// 동행 매칭의 일정 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

import java.time.LocalDateTime;

public record CompanionSchedule(
		Long id,
		Long matchId,
		Long placeId,
		LocalDateTime scheduledAt,
		Integer estimatedDurationMinutes,
		boolean confirmed
) {
}
