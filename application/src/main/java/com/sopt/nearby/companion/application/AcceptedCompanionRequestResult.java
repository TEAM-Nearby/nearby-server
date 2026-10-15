// 동행 신청 수락 결과를 웹 어댑터로 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import java.time.LocalDateTime;

public record AcceptedCompanionRequestResult(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        Long matchId,
        CompanionMatchStatus matchStatus,
        LocalDateTime meetingAt
) {
}
