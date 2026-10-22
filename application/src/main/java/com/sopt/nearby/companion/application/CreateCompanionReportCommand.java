// 동행 신고 유스케이스 입력값을 표현하는 명령 객체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.report.ReportReason;
import java.util.List;

public record CreateCompanionReportCommand(
		Long reporterUserId,
		Long meetingId,
		Long matchId,
		Long reportedUserId,
		List<ReportReason> reasons,
		String detail
) {
	public CreateCompanionReportCommand(
			final Long reporterUserId,
			final Long meetingId,
			final Long reportedUserId,
			final List<ReportReason> reasons,
			final String detail
	) {
		this(reporterUserId, meetingId, null, reportedUserId, reasons, detail);
	}

	public static CreateCompanionReportCommand forMatch(
			final Long reporterUserId,
			final Long matchId,
			final Long reportedUserId,
			final List<ReportReason> reasons,
			final String detail
	) {
		return new CreateCompanionReportCommand(reporterUserId, null, matchId, reportedUserId, reasons, detail);
	}
}
