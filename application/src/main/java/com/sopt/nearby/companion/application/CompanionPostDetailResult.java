// 동행 모집 글 상세 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.post.CompanionPostApplyStatus;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;

public record CompanionPostDetailResult(
        Long postId,
        Long hostUserId,
        Long hostProfileId,
        String googlePlaceId,
        CompanionCity city,
        ZonedDateTime currentLocalTime,
        LocalDateTime meetingAt,
        int maxParticipants,
        String content,
        String openChatUrl,
        CompanionPostStatus status,
        LocalDateTime createdAt,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime expiresAt,
        int participantCount,
        List<Participant> participants,
        CompanionPostApplyStatus applyStatus,
        Place place,
        HostProfileSummary hostProfileSummary
) {

    public CompanionPostDetailResult {
        participants = participants == null ? List.of() : List.copyOf(participants);
    }

	public CompanionPostDetailResult(
			final Long postId,
			final Long hostUserId,
			final Long hostProfileId,
			final String googlePlaceId,
			final LocalDateTime meetingAt,
			final int maxParticipants,
			final String content,
			final String openChatUrl,
			final CompanionPostStatus status,
			final LocalDateTime createdAt,
			final CompanionPostMeetingTimeType meetingTimeType,
			final LocalDateTime expiresAt,
			final int participantCount,
			final List<Participant> participants,
			final CompanionPostApplyStatus applyStatus,
			final Place place,
			final HostProfileSummary hostProfileSummary
	) {
		this(
				postId,
				hostUserId,
				hostProfileId,
				googlePlaceId,
				null,
				null,
				meetingAt,
				maxParticipants,
				content,
				openChatUrl,
				status,
				createdAt,
				meetingTimeType,
				expiresAt,
				participantCount,
				participants,
				applyStatus,
				place,
				hostProfileSummary
		);
	}

    public record Participant(
            Long userId,
            String profileImageUrl
    ) {
    }

    public record Place(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            CompanionPostPlaceCategory category
    ) {
    }

    public record HostProfileSummary(
            Long profileId,
            String nickname,
            String intro,
            UserGender gender,
            Integer birthYear,
            String profileImageUrl,
            BigDecimal mannerScore,
            List<ReviewKeyword> mannerKeywords,
            LocalDateTime phoneVerifiedAt,
            List<TravelStyleKeyword> keywords
    ) {

        public HostProfileSummary {
            mannerKeywords = mannerKeywords == null ? List.of() : List.copyOf(mannerKeywords);
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
        }
    }
}
