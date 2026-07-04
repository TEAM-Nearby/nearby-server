package com.sopt.nearby.companion.domain.model.match;

import java.time.LocalDateTime;

public record CompanionMatchSummary(
        Long matchId,
        String hostNickname,
        String placeName,
        LocalDateTime meetingAt,
        String content,
        CompanionMatchStatus matchStatus
) {
}
