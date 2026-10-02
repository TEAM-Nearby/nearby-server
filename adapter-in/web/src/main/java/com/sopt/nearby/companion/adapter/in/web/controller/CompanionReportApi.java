// 동행 신고 API 문서를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionReportRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionReportResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReportContextNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReportCurrentUserAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionReportCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionReportMeetingNotOngoingException;
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
			InvalidCompanionReportReasonException.class,
			CompanionReportMeetingNotOngoingException.class,
			CompanionReportCurrentUserNotCheckedInException.class,
			CompanionReportCurrentUserAlreadyCompletedException.class,
			CompanionReportSelfNotAllowedException.class,
			CompanionReportAlreadyExistsException.class,
			ForbiddenCompanionReportException.class,
			CompanionReportTargetNotFoundException.class,
			CompanionReportContextNotFoundException.class,
			CompanionMeetingNotFoundException.class
	})
	@Operation(
			summary = "동행 사용자 신고",
			description = "동행 중이며 현재 사용자가 동행 마치기를 누르기 전 상대방을 신고합니다.",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	CommonResponse<CreateCompanionReportResponse> createReport(
			@Parameter(description = "신고할 동행 만남 ID", required = true, example = "1")
			Long meetingId,
			CreateCompanionReportRequest request,
			@Parameter(hidden = true)
			Principal principal
	);
}
