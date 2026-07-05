//매칭된 동행 API
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.dto.request.CompanionMatchScheduleRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchScheduleResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchesResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleAlreadyConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionScheduleRequestException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.PathVariable;


@Tag(name = "CompanionMatch", description = "매칭된 동행 API")
public interface CompanionMatchApi {

    @Operation(
            summary = "매칭된 동행 목록 보기",
            description = "JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionMatchesResponse> getMatches(
            final Principal principal
    );


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


    @ApiExceptions({
            InvalidCompanionScheduleRequestException.class,
            ForbiddenCompanionScheduleException.class,
            CompanionMatchNotFoundException.class,
            CompanionScheduleAlreadyConfirmedException.class,
            CompanionMatchAlreadyCanceledException.class,
            CompanionMatchAlreadyCompletedException.class
    })
    @Operation(
            summary = "글 작성자의 동행 일정 확정",
            description = "JWT 액세스 토큰으로 인증된 사용자면서 글 작성자일 때 동행 일정 확정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionMatchScheduleResponse> postSchedule(
            @Parameter(description = "확정할 매칭 ID", required = true, example = "1")
            Long matchId,
            CompanionMatchScheduleRequest request,

            @Parameter(hidden = true)
            Principal principal
    );
}
