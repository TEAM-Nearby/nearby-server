// 매칭된 동행 목록 조회 응답에 사용되는 매칭 요약 도메인 모델
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
