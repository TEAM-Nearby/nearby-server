//매칭된 동행 API
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.dto.request.CompanionMatchScheduleRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchScheduleResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchesResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionScheduleDetailResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleAlreadyConfirmedException;
import com.sopt.nearby.companion.domain.exception.CompletedCompanionScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionScheduleException;
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

    @ApiExceptions({
            InvalidCompanionMatchIdException.class,
            CompanionMatchNotFoundException.class,
            ForbiddenReadCompanionScheduleException.class,
            CompanionMatchScheduleNotReadableException.class,
            CompletedCompanionScheduleNotReadableException.class
    })
    @Operation(
            summary = "내 동행 일정 조회",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭의 동행 일정 정보를 조회합니다.
                    성공 응답은 MATCHED 또는 SCHEDULE_CONFIRMED 상태만 반환하며, 로그인 사용자의 닉네임과 모집글의 만남 시간 유형을 함께 반환합니다.
                    meetingTimeType이 NOW인 모집글은 신청 수락 시 일정이 자동 확정되어 SCHEDULE_CONFIRMED 상태로 반환됩니다.
                    NOW 일정의 schedule.place는 모집글 장소, schedule.scheduledAt은 즉시 동행 노출 만료 시간입니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionScheduleDetailResponse> getSchedule(
            @Parameter(description = "일정을 조회할 매칭 ID", required = true, example = "1")
            Long matchId,
            @Parameter(hidden = true)
            Principal principal
    );
}
