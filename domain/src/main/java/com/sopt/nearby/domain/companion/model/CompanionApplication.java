// 동행 모집글 신청 정보를 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

import java.time.LocalDateTime;

public record CompanionApplication(
		Long id,
		Long postId,
		Long applicantUserId,
		CompanionApplicationStatus status,
		String rejectionReason,
		LocalDateTime createdAt
) {
}
