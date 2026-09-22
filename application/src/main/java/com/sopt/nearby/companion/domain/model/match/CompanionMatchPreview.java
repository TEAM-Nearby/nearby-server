// 동행 매칭 미리보기 응답용 도메인 모델
package com.sopt.nearby.companion.domain.model.match;


import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public record CompanionMatchPreview(
        Long matchId,
        Member host,
        List<Member> members,
        Post companionPost
) {

    public record Member(
            Long userId,
            String profileImageUrl,
            String nickname
    ) {
    }

    public record Post(
            Long postId,
            String content,
            String placeName,
            String placeAddress,
            CompanionCity city,
            ZonedDateTime currentLocalTime,
            CompanionPostMeetingTimeType meetingTimeType,
            LocalDateTime meetingAt
    ) {

		public Post(
				final Long postId,
				final String content,
				final String placeName,
				final CompanionPostMeetingTimeType meetingTimeType,
				final LocalDateTime meetingAt
		) {
			this(postId, content, placeName, null, null, null, meetingTimeType, meetingAt);
		}

    }
}
