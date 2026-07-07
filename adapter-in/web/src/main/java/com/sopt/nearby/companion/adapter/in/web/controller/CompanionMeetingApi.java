// 동행 만남 API 문서를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.CheckInCompanionMeetingRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CheckInCompanionMeetingResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.OngoingCompanionMeetingsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMeetingDetailResponse;
import com.sopt.nearby.companion.domain.exception.CheckInTimeNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleNotConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionMeeting", description = "동행 만남 API")
public interface CompanionMeetingApi {

    @Operation(
            summary = "현재 진행 중인 동행 목록 조회",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 ONGOING 만남 목록을 조회합니다.
                    companion은 항상 HOST 프로필이며, 로그인 사용자가 HOST여도 자기 자신의 HOST 프로필을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<OngoingCompanionMeetingsResponse> getOngoingMeetings(
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidCompanionMeetingIdException.class,
            ForbiddenReadCompanionMeetingException.class,
            CompanionMeetingNotFoundException.class,
            ReadCompanionMeetingAlreadyCanceledException.class,
            ReadCompanionMeetingAlreadyCompletedException.class
    })
    @Operation(
            summary = "진행 중인 동행 상세 조회",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 ONGOING 동행 만남 상세 정보를 조회합니다.
                    호스트 한 명의 프로필을 반환하며, 로그인 사용자가 HOST여도 HOST 본인의 프로필을 반환합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionMeetingDetailResponse> getDetail(
            @Parameter(description = "상세 조회할 진행 중인 동행 만남 ID", required = true, example = "1")
            Long meetingId,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidCheckInRequestException.class,
            OutOfCheckInRadiusException.class,
            ForbiddenCompanionMeetingException.class,
            CompanionMeetingNotFoundException.class,
            CompanionMeetingAlreadyCanceledException.class,
            CompanionMeetingAlreadyCompletedException.class,
            CompanionScheduleNotConfirmedException.class,
            CheckInTimeNotAllowedException.class
    })
    @Operation(
            summary = "만남 인증",
            description = "JWT 액세스 토큰으로 인증된 사용자가 진행 중인 동행 만남의 장소 반경 안에서 만남을 인증합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CheckInCompanionMeetingResponse> checkIn(
            @Parameter(description = "인증할 진행 중인 동행 만남 ID", required = true, example = "1")
            Long meetingId,
            CheckInCompanionMeetingRequest request,
            @Parameter(hidden = true)
            Principal principal
    );
}
