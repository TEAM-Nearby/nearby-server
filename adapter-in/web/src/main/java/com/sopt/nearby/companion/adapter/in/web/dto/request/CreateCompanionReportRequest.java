// 동행 신고 등록 요청 본문을 유스케이스 명령으로 변환하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.CreateCompanionReportCommand;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportReasonException;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Locale;

public record CreateCompanionReportRequest(
		@Schema(description = "신고 대상 사용자 ID. 호스트가 detail에 참여자 닉네임을 입력하는 경우 생략 가능", nullable = true)
		Long reportedUserId,
		List<String> reasons,
		@Schema(description = "상세 내용. reportedUserId를 생략한 호스트 신고에서는 참여자 닉네임 필수", nullable = true)
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
