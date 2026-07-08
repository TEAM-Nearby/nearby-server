// 내가 작성한 동행 모집글 목록 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReadMyCompanionPostsResult(
		List<Post> posts
) {

	public ReadMyCompanionPostsResult {
		posts = posts == null ? List.of() : List.copyOf(posts);
	}

	public record Post(
			Long postId,
			String cityName,
			LocalDateTime scheduledAt,
			Place place,
			int currentParticipants,
			int maxParticipants,
			String content,
			List<ReviewKeyword> reviewKeywords
	) {

		public Post {
			reviewKeywords = reviewKeywords == null ? List.of() : List.copyOf(reviewKeywords);
		}
	}

	public record Place(
			String googlePlaceId,
			String name,
			BigDecimal latitude,
			BigDecimal longitude
	) {
	}
}
