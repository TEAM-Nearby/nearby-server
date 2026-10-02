// 동행 신고 등록 요청 본문을 유스케이스 명령으로 변환하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.CreateCompanionReportCommand;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportReasonException;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import java.util.List;
import java.util.Locale;

public record CreateCompanionReportRequest(
		Long reportedUserId,
		List<String> reasons,
		String detail
) {

	public CreateCompanionReportCommand toCommand(final Long meetingId, final Long reporterUserId) {
		return new CreateCompanionReportCommand(
				reporterUserId,
				meetingId,
				reportedUserId,
				parseReasons(reasons),
				detail
		);
	}

	private List<ReportReason> parseReasons(final List<String> values) {
		if (values == null) {
			return null;
		}
		return values.stream().map(this::parseReason).toList();
	}

	private ReportReason parseReason(final String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidCompanionReportReasonException();
		}
		try {
			return ReportReason.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new InvalidCompanionReportReasonException();
		}
	}
}
