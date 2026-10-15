// 동행 신고 유스케이스 입력값을 표현하는 명령 객체
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.report.ReportReason;
import java.util.List;

public record CreateCompanionReportCommand(
		Long reporterUserId,
		Long meetingId,
		Long reportedUserId,
		List<ReportReason> reasons,
		String detail
) {
}
