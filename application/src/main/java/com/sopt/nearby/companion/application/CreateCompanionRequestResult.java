// 동행 신청 생성 결과를 웹 어댑터로 전달하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import java.time.LocalDateTime;

public record CreateCompanionRequestResult(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        LocalDateTime createdAt
) {
}
