// 내가 작성한 동행 모집글 목록 조회용 요약 모델이다.
package com.sopt.nearby.companion.domain.model.post;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MyCompanionPostSummary(
		Long postId,
		LocalDateTime scheduledAt,
		Place place,
		int currentParticipants,
		int maxParticipants,
		String content,
		List<ReviewKeyword> reviewKeywords
) {

	public MyCompanionPostSummary {
		reviewKeywords = reviewKeywords == null ? List.of() : List.copyOf(reviewKeywords);
	}

	public record Place(
			String googlePlaceId,
			String name,
			String address,
			BigDecimal latitude,
			BigDecimal longitude
	) {
	}
}
