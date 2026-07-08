// 동행 신청 검토 화면 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionRequestReviewResponse;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionRequestHostOnlyException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionRequest", description = "동행 신청 API")
public interface CompanionRequestApi {

    @ApiExceptions({
            ForbiddenCompanionRequestHostOnlyException.class,
            CompanionRequestNotFoundException.class
    })
    @Operation(
            summary = "동행 신청 검토 화면 상세 조회",
            description = "JWT 액세스 토큰으로 인증된 호스트가 동행 신청 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionRequestReviewResponse> getReview(
            @Parameter(description = "동행 신청 ID", required = true, example = "3")
            Long applicationId,
            @Parameter(hidden = true)
            Principal principal
    );
}
