// 동행 미팅 신고 정보를 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

import java.time.LocalDateTime;

public record CompanionReport(
		Long id,
		Long meetingId,
		Long reporterUserId,
		Long reportedUserId,
		String detail,
		LocalDateTime createdAt
) {
}
