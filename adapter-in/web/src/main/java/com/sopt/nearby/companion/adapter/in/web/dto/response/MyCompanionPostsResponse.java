// 내가 작성한 동행 모집글 목록 조회 HTTP 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadMyCompanionPostsResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MyCompanionPostsResponse(
		List<PostResponse> posts
) {

	public static MyCompanionPostsResponse from(final ReadMyCompanionPostsResult result) {
		return new MyCompanionPostsResponse(
				result.posts().stream()
						.map(PostResponse::from)
						.toList()
		);
	}

	public record PostResponse(
			Long postId,
			String cityName,
			LocalDateTime scheduledAt,
			PlaceResponse place,
			int currentParticipants,
			int maxParticipants,
			String content,
			List<String> reviewKeywords
	) {

		static PostResponse from(final ReadMyCompanionPostsResult.Post post) {
			return new PostResponse(
					post.postId(),
					post.cityName(),
					post.scheduledAt(),
					PlaceResponse.from(post.place()),
					post.currentParticipants(),
					post.maxParticipants(),
					post.content(),
					post.reviewKeywords().stream()
							.map(Enum::name)
							.toList()
			);
		}
	}

	public record PlaceResponse(
			String googlePlaceId,
			String name,
			BigDecimal latitude,
			BigDecimal longitude
	) {

		static PlaceResponse from(final ReadMyCompanionPostsResult.Place place) {
			return new PlaceResponse(
					place.googlePlaceId(),
					place.name(),
					place.latitude(),
					place.longitude()
			);
		}
	}
}
