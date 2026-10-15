// 동행 신청 거절 결과를 웹 어댑터로 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;

public record RejectedCompanionRequestResult(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus
) {
}
