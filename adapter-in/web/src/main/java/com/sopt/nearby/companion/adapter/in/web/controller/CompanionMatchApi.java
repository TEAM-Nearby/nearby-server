//매칭된 동행 API
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;


@Tag(name = "CompanionMatch", description = "매칭된 동행 API")
public interface CompanionMatchApi {

    @ApiExceptions({
            InvalidCompanionMatchIdException.class,
            CompanionMatchNotFoundException.class,
            ForbiddenCompanionMatchException.class,
            CompanionPostNotFoundException.class,
            CompanionProfileNotFoundException.class
    })
    @Operation(
            summary = "매칭된 동행 미리보기",
            description = "JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭의 미리보기를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionMatchPreviewResponse> getPreview(
            @Parameter(description = "조회할 매칭 ID", required = true, example = "1")
            Long matchId,
            @Parameter(hidden = true)
            Principal principal
    );
}
