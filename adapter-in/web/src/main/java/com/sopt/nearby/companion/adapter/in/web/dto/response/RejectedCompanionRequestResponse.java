// 동행 신청 거절 결과를 클라이언트에 반환한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.RejectedCompanionRequestResult;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;

public record RejectedCompanionRequestResponse(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus
) {

    public static RejectedCompanionRequestResponse from(final RejectedCompanionRequestResult result) {
        return new RejectedCompanionRequestResponse(
                result.applicationId(),
                result.postId(),
                result.applicationStatus()
        );
    }
}
