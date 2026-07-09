// 매칭된 동행 목록의 단일 항목 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;

public record CompanionMatchResponse(
        Long matchId,
        String hostNickname,
        String hostProfileImageUrl,
        UserGender hostGender,
        String placeName,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime createdAt,
        String content,
        CompanionMatchStatus matchStatus
) {

    public static CompanionMatchResponse from(final CompanionMatchSummary summary) {
        return new CompanionMatchResponse(
                summary.matchId(),
                summary.hostNickname(),
                summary.hostProfileImageUrl(),
                summary.hostGender(),
                summary.placeName(),
                summary.meetingAt(),
                summary.meetingTimeType(),
                summary.createdAt(),
                summary.content(),
                summary.matchStatus()
        );
    }
}
