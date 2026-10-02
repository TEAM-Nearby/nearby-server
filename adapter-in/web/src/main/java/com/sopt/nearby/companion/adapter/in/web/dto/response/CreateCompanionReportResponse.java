// 동행 신고 등록 결과를 HTTP 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CreateCompanionReportResult;
import java.time.LocalDateTime;

public record CreateCompanionReportResponse(
		Long meetingId,
		Long reportId,
		LocalDateTime reportedAt
) {

	public static CreateCompanionReportResponse from(final CreateCompanionReportResult result) {
		return new CreateCompanionReportResponse(result.meetingId(), result.reportId(), result.reportedAt());
	}
}
