// 동행 모집글의 핵심 속성을 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.post;

import java.time.LocalDateTime;

public record CompanionPost(
		Long id,
		Long hostUserId,
		Long placeId,
		CompanionPostMeetingTimeType meetingTimeType,
		LocalDateTime meetingAt,
		LocalDateTime exposureExpiresAt,
		int maxParticipants,
		boolean departEvenIfNotFull,
		String content,
		String openChatUrl,
		CompanionPostStatus status,
		LocalDateTime createdAt
) {

	public CompanionPost(
			final Long id,
			final Long hostUserId,
			final Long placeId,
			final LocalDateTime meetingAt,
			final int maxParticipants,
			final String content,
			final String openChatUrl,
			final CompanionPostStatus status,
			final LocalDateTime createdAt
	) {
		this(
				id,
				hostUserId,
				placeId,
				CompanionPostMeetingTimeType.SCHEDULED,
				meetingAt,
				null,
				maxParticipants,
				true,
				content,
				openChatUrl,
				status,
				createdAt
		);
	}
}
