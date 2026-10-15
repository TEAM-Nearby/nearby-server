// 동행 신고 HTTP 요청을 처리하는 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionReportRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionReportResponse;
import com.sopt.nearby.companion.application.CreateCompanionReportResult;
import com.sopt.nearby.companion.port.in.CreateCompanionReportUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-meetings")
public class CompanionReportController implements CompanionReportApi {

	private final CreateCompanionReportUseCase createCompanionReportUseCase;

	public CompanionReportController(final CreateCompanionReportUseCase createCompanionReportUseCase) {
		this.createCompanionReportUseCase = createCompanionReportUseCase;
	}

	@Override
	@PostMapping("/{meetingId}/reports")
	@ResponseStatus(HttpStatus.CREATED)
	public CommonResponse<CreateCompanionReportResponse> createReport(
			@PathVariable final Long meetingId,
			@RequestBody(required = false) final CreateCompanionReportRequest request,
			final Principal principal
	) {
		Long userId = Long.valueOf(principal.getName());
		CreateCompanionReportResult result = createCompanionReportUseCase.create(
				request == null ? null : request.toCommand(meetingId, userId)
		);

		return CommonResponse.created(
				CompanionSuccessCode.CREATE_COMPANION_REPORT,
				CreateCompanionReportResponse.from(result)
		);
	}
}
