// 동행 신청 수락 결과를 클라이언트에 반환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.AcceptedCompanionRequestResult;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;

public record AcceptedCompanionRequestResponse(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        Long matchId,
        CompanionMatchStatus matchStatus
) {

    public static AcceptedCompanionRequestResponse from(final AcceptedCompanionRequestResult result) {
        return new AcceptedCompanionRequestResponse(
                result.applicationId(),
                result.postId(),
                result.applicationStatus(),
                result.matchId(),
                result.matchStatus()
        );
    }
}
