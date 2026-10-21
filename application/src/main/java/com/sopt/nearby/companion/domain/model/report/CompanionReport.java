// 매칭 기준의 동행 신고와 선택적인 만남 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model.report;

import java.time.LocalDateTime;

public record CompanionReport(
		Long id,
		Long matchId,
		Long meetingId,
		Long reporterUserId,
		Long reportedUserId,
		String detail,
		LocalDateTime createdAt
) {
}
