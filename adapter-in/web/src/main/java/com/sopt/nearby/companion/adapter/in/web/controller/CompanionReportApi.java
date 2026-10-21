// 동행 신고 API 문서를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionReportRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionReportResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyCanceledException;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionMatchReportResponse;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReportContextNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportSelfNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionReportTargetNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReportException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportReasonException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionReportRequestException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionReport", description = "동행 신고 API")
public interface CompanionReportApi {

	@ApiExceptions({
			InvalidCompanionReportRequestException.class,
			CompanionReportAlreadyCanceledException.class,
			CompanionMatchNotFoundException.class,
			InvalidCompanionReportReasonException.class,
			CompanionReportSelfNotAllowedException.class,
			CompanionReportAlreadyExistsException.class,
			ForbiddenCompanionReportException.class,
			CompanionReportTargetNotFoundException.class,
			CompanionReportContextNotFoundException.class,
			CompanionMeetingNotFoundException.class
	})
	@Operation(
			summary = "동행 사용자 신고",
			description = """
					같은 매칭의 참여자를 만남 인증 전부터 동행 완료 및 후기 작성 후까지 신고할 수 있습니다.
					취소된 동행은 신고할 수 없으며 같은 매칭의 동일 상대에 대한 중복 신고는 차단합니다.
					호스트는 reportedUserId를 생략하고 detail에 신고 대상 참여자의 정확한 닉네임을 입력할 수 있습니다.
					""",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<CreateCompanionReportResponse> createReport(
			@Parameter(description = "신고할 동행 만남 ID", required = true, example = "1")
			Long meetingId,
			CreateCompanionReportRequest request,
			@Parameter(hidden = true)
			Principal principal
	);

	@ApiExceptions({
			InvalidCompanionReportRequestException.class,
			CompanionReportAlreadyCanceledException.class,
			CompanionMatchNotFoundException.class,
			InvalidCompanionReportReasonException.class,
			CompanionReportSelfNotAllowedException.class,
			CompanionReportAlreadyExistsException.class,
			ForbiddenCompanionReportException.class,
			CompanionReportTargetNotFoundException.class,
			CompanionReportContextNotFoundException.class
	})
	@Operation(
			summary = "매칭 기준 동행 사용자 신고",
			description = """
					일정 미확정 상태를 포함해 같은 매칭의 참여자를 만남 인증 전부터 동행 완료 및 후기 작성 후까지 신고할 수 있습니다.
					취소된 동행은 신고할 수 없으며 같은 매칭의 동일 상대에 대한 중복 신고는 차단합니다.
					호스트는 reportedUserId를 생략하고 detail에 신고 대상 참여자의 정확한 닉네임을 입력할 수 있습니다.
					""",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<CreateCompanionMatchReportResponse> createMatchReport(
			@Parameter(description = "신고할 동행 매칭 ID", required = true, example = "1")
			Long matchId,
			CreateCompanionReportRequest request,
			@Parameter(hidden = true)
			Principal principal
	);
}
