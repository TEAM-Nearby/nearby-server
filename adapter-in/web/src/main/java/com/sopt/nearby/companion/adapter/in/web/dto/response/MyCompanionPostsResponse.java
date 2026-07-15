// 내가 작성한 동행 모집글 목록 조회 HTTP 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadMyCompanionPostsResult;
import io.swagger.v3.oas.annotations.media.Schema;
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
			@Schema(
					description = "모집글 작성자 프로필 이미지 URL, 이미지가 없으면 null",
					example = "https://cdn.nearby.com/profiles/1.jpg",
					nullable = true,
					requiredMode = Schema.RequiredMode.REQUIRED
			)
			String hostProfileImageUrl,
			@Schema(
					description = "호스트를 제외한 참여 확정 멤버 목록, 참여자가 없으면 빈 배열",
					requiredMode = Schema.RequiredMode.REQUIRED
			)
			List<MemberResponse> members,
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
					post.hostProfileImageUrl(),
					post.members().stream()
							.map(MemberResponse::from)
							.toList(),
					post.currentParticipants(),
					post.maxParticipants(),
					post.content(),
					post.reviewKeywords().stream()
							.map(Enum::name)
							.toList()
			);
		}
	}

	public record MemberResponse(
			@Schema(
					description = "참여 멤버 사용자 ID",
					example = "2",
					requiredMode = Schema.RequiredMode.REQUIRED
			)
			Long userId,
			@Schema(
					description = "참여 멤버 프로필 이미지 URL, 이미지가 없으면 null",
					example = "https://cdn.nearby.com/profiles/2.jpg",
					nullable = true,
					requiredMode = Schema.RequiredMode.REQUIRED
			)
			String profileImageUrl
	) {

		static MemberResponse from(final ReadMyCompanionPostsResult.Member member) {
			return new MemberResponse(member.userId(), member.profileImageUrl());
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
