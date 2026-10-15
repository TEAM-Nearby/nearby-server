// 동행 신고 유스케이스 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import java.time.LocalDateTime;

public record CreateCompanionReportResult(
		Long meetingId,
		Long reportId,
		LocalDateTime reportedAt
) {
}
