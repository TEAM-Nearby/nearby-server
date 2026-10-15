// 내가 작성한 동행 모집글 목록 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public record ReadMyCompanionPostsResult(
		List<Post> posts
) {

	public ReadMyCompanionPostsResult {
		posts = posts == null ? List.of() : List.copyOf(posts);
	}

	public record Post(
			Long postId,
			String cityNameKor,
			CompanionCity city,
			ZonedDateTime currentLocalTime,
			LocalDateTime scheduledAt,
			Place place,
			String hostProfileImageUrl,
			List<Member> members,
			int currentParticipants,
			int maxParticipants,
			String content,
			List<ReviewKeyword> reviewKeywords
	) {

		public Post {
			members = members == null ? List.of() : List.copyOf(members);
			reviewKeywords = reviewKeywords == null ? List.of() : List.copyOf(reviewKeywords);
		}

		public Post(
				final Long postId,
				final String cityNameKor,
				final LocalDateTime scheduledAt,
				final Place place,
				final String hostProfileImageUrl,
				final List<Member> members,
				final int currentParticipants,
				final int maxParticipants,
				final String content,
				final List<ReviewKeyword> reviewKeywords
		) {
			this(
					postId,
					cityNameKor,
					null,
					null,
					scheduledAt,
					place,
					hostProfileImageUrl,
					members,
					currentParticipants,
					maxParticipants,
					content,
					reviewKeywords
			);
		}
	}

	public record Member(
			Long userId,
			String profileImageUrl
	) {
	}

	public record Place(
			String googlePlaceId,
			String name,
			BigDecimal latitude,
			BigDecimal longitude
	) {
	}
}
