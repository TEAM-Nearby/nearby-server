// 동행 모집 글 상세 조회에 필요한 조인 결과를 표현한다.
package com.sopt.nearby.companion.domain.model.post;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionPostDetail(
        Long postId,
        Long hostUserId,
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
        CompanionApplicationStatus applicationStatus,
        Place place,
        HostProfileSummary hostProfileSummary
) {

    public CompanionPostDetail {
        participants = participants == null ? List.of() : List.copyOf(participants);
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
            LocalDateTime phoneVerifiedAt,
            List<TravelStyleKeyword> keywords
    ) {

        public HostProfileSummary {
            keywords = keywords == null ? List.of() : List.copyOf(keywords);
        }
    }
}
