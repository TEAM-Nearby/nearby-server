// 동행 모집글의 핵심 속성을 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

import java.time.LocalDateTime;

public record CompanionPost(
		Long id,
		Long hostUserId,
		Long placeId,
		LocalDateTime meetingAt,
		int maxParticipants,
		String content,
		String openChatUrl,
		CompanionPostStatus status,
		LocalDateTime createdAt
) {
}
