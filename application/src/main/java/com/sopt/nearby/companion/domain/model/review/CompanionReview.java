// 동행 미팅 리뷰 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.review;

import java.time.LocalDateTime;

public record CompanionReview(
		Long id,
		Long meetingId,
		Long reviewerUserId,
		Long revieweeUserId,
		int rating,
		LocalDateTime createdAt
) {
}
