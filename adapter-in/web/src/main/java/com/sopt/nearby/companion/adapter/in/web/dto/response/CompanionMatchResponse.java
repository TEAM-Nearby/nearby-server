// 매칭된 동행 목록의 단일 항목 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import java.time.LocalDateTime;

public record CompanionMatchResponse(
        Long matchId,
        String hostNickname,
        String placeName,
        LocalDateTime meetingAt,
        String content,
        CompanionMatchStatus matchStatus
) {

    public static CompanionMatchResponse from(final CompanionMatchSummary summary) {
        return new CompanionMatchResponse(
                summary.matchId(),
                summary.hostNickname(),
                summary.placeName(),
                summary.meetingAt(),
                summary.content(),
                summary.matchStatus()
        );
    }
}