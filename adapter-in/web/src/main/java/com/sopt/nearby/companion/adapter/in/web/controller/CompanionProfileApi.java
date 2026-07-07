// 동행 프로필 상세 조회 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionProfileResponse;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionProfile", description = "동행 프로필 API")
public interface CompanionProfileApi {

    @ApiExceptions({
            CompanionProfileNotFoundException.class
    })
    @Operation(
            summary = "동행 프로필 상세 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 ACTIVE 상태의 동행 프로필 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionProfileResponse> getProfile(
            @Parameter(description = "조회할 동행 프로필 ID", required = true, example = "5")
            Long profileId,
            @Parameter(hidden = true)
            Principal principal
    );
}
