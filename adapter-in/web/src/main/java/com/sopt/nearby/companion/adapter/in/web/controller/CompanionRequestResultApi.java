// 신청자의 동행 신청 결과 조회 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionRequestResultResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionRequestResultNotReadyException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionRequestResult", description = "신청자 동행 신청 결과 API")
public interface CompanionRequestResultApi {

    @ApiExceptions({
            CompanionRequestNotFoundException.class,
            CompanionRequestResultNotReadyException.class,
            CompanionRequestResultNotReadableException.class,
            CompanionMatchNotFoundException.class
    })
    @Operation(
            summary = "동행 신청 결과 조회",
            description = "JWT 액세스 토큰으로 인증된 신청자가 자신의 동행 신청 처리 결과를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionRequestResultResponse> getResult(
            @Parameter(description = "동행 신청 ID", required = true, example = "3")
            Long applicationId,
            @Parameter(hidden = true)
            Principal principal
    );
}
