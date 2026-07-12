// 동행 만남 API 문서를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.CheckInCompanionMeetingRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionReviewsRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CheckInCompanionMeetingResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompleteCompanionMeetingResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMeetingDetailResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionReviewTargetsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionReviewsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.OngoingCompanionMeetingsResponse;
import com.sopt.nearby.companion.domain.exception.CannotReviewSelfException;
import com.sopt.nearby.companion.domain.exception.CheckInTimeNotAllowedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompleteCompanionMeetingCurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewAlreadyExistsException;
import com.sopt.nearby.companion.domain.exception.CompanionReviewMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionScheduleNotConfirmedException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReviewException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionReviewTargetException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompleteCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCheckInRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordCountException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewRatingException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidReviewTargetException;
import com.sopt.nearby.companion.domain.exception.OutOfCheckInRadiusException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.exception.CurrentUserNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.RevieweeNotCheckedInException;
import com.sopt.nearby.companion.domain.exception.RevieweeNotFoundException;
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
                    meetingTimeType을 함께 반환하며, NOW 타입의 meetingAt은 즉시 만남의 exposureExpiresAt을 사용합니다.
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
                    meetingTimeType을 함께 반환하며, NOW 타입의 meetingAt은 즉시 만남의 exposureExpiresAt을 사용합니다.
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
            InvalidCompanionMeetingIdException.class,
            ForbiddenCompanionReviewTargetException.class,
            CompanionMeetingNotFoundException.class,
            CurrentUserNotCheckedInException.class,
            CompanionReviewMeetingAlreadyCanceledException.class
    })
    @Operation(
            summary = "동행 후기 대상 목록 조회",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 동행의 후기 작성 대상 목록을 조회합니다.
                    HOST는 체크인을 완료한 GUEST 목록을 조회하고, GUEST는 체크인을 완료한 HOST만 조회합니다.
                    완료된 동행에서도 체크인과 대상 조건을 만족하면 후기를 이어서 작성할 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionReviewTargetsResponse> getReviewTargets(
            @Parameter(description = "후기 대상 목록을 조회할 동행 만남 ID", required = true, example = "1")
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

    @ApiExceptions({
            InvalidCompanionMeetingIdException.class,
            ForbiddenCompleteCompanionMeetingException.class,
            CompanionMeetingNotFoundException.class,
            CompleteCompanionMeetingCurrentUserNotCheckedInException.class,
            CompleteCompanionMeetingAlreadyCanceledException.class,
            CompleteCompanionMeetingAlreadyCompletedException.class
    })
    @Operation(
            summary = "동행 마치기",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자의 개인 동행 완료를 기록합니다.
                    현재 사용자가 해당 동행 참여자이고 만남 인증을 완료했다면 HOST/GUEST 역할과 리뷰 작성 여부에 관계없이 동행을 마칠 수 있습니다.
                    모든 매칭 참여자가 개인 완료하면 동행 만남과 매칭 상태를 COMPLETED로 변경하며, 개인 또는 전체 완료 후에도 후기를 작성할 수 있습니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompleteCompanionMeetingResponse> complete(
            @Parameter(description = "완료 처리할 동행 만남 ID", required = true, example = "1")
            Long meetingId,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidReviewRequestException.class,
            InvalidReviewTargetException.class,
            InvalidReviewRatingException.class,
            InvalidReviewKeywordException.class,
            InvalidReviewKeywordCountException.class,
            CannotReviewSelfException.class,
            ForbiddenCompanionReviewException.class,
            CompanionMeetingNotFoundException.class,
            RevieweeNotFoundException.class,
            CompanionReviewAlreadyExistsException.class,
            CurrentUserNotCheckedInException.class,
            RevieweeNotCheckedInException.class,
            CompanionReviewMeetingAlreadyCanceledException.class
    })
    @Operation(
            summary = "동행 후기 등록",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 체크인을 완료한 동행 참여자 한 명에게 후기를 등록합니다.
                    HOST는 체크인을 완료한 GUEST에게 대상별로 후기를 등록할 수 있고, GUEST는 HOST에게만 후기를 등록할 수 있습니다.
                    후기 대상 revieweeUserId는 진행 중 목록의 matchId로 매칭 미리보기를 조회한 뒤 members[].memberId에서 선택합니다.
                    동일 만남에서 같은 reviewer-reviewee 조합은 한 번만 등록할 수 있으며, 동행 완료 처리는 별도 동행 마치기 API에서 수행합니다.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CreateCompanionReviewsResponse> createReviews(
            @Parameter(description = "후기를 등록할 동행 만남 ID", required = true, example = "1")
            Long meetingId,
            CreateCompanionReviewsRequest request,
            @Parameter(hidden = true)
            Principal principal
    );
}
