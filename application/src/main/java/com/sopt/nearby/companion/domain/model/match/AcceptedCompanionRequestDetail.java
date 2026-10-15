// 수락된 동행 신청 결과에 필요한 상세 조회 모델
package com.sopt.nearby.companion.domain.model.match;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcceptedCompanionRequestDetail(
        Long matchId,
        CompanionMatchStatus matchStatus,
        Long postId,
        Host host,
        Place place,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt,
        int participantCount,
        int maxParticipants,
        String openChatUrl
) {

    public record Host(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {
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
