//매칭된 동행 API
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.dto.request.CompanionMatchScheduleRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchScheduleResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchesResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionScheduleDetailResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMatchAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionMatch", description = "매칭된 동행 API")
public interface CompanionMatchApi {

    @Operation(
            summary = "매칭된 동행 목록 보기",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭 목록을 조회합니다.
                    MATCHED 또는 SCHEDULE_CONFIRMED 상태만 반환합니다.
                    meetingTimeType이 NOW인 신규 수락 흐름은 일정이 자동 확정되어 SCHEDULE_CONFIRMED 상태로 반환됩니다.
                    createdAt과 meetingAt의 화면 표기 문구는 클라이언트에서 포맷팅합니다.
                    """,
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
    @ApiResponse(
            responseCode = "200",
            description = "매칭된 동행 미리보기 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "READ_COMPANION_MATCH_PREVIEW",
                            value = """
                                    {
                                      "status": 200,
                                      "code": "READ_COMPANION_MATCH_PREVIEW",
                                      "message": "매칭된 동행 미리보기 정보를 조회했어요.",
                                      "data": {
                                        "matchId": 1,
                                        "host": {
                                          "hostName": "조로",
                                          "hostProfileImageUrl": "https://image.url/hostProfile.png"
                                        },
                                        "members": [
                                          {
                                            "memberId": 2,
                                            "profileImageUrl": "https://image.url/profile.png",
                                            "nickname": "김루피"
                                          }
                                        ],
                                        "companionPost": {
                                          "postId": 1,
                                          "content": "오늘 저녁 바르셀로나에서 같이 타파스 드실 분 구해요",
                                          "placeName": "바르셀로나 고딕 지구",
                                          "meetingTimeType": "NOW",
                                          "meetingAt": "2026-07-05T13:00:00"
                                        }
                                      }
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "매칭된 동행 미리보기",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭의 미리보기를 조회합니다.
                    host에는 모집글 작성자의 닉네임과 프로필 이미지 URL을 반환하고, members에는 호스트를 제외한 참여자를 반환합니다.
                    companionPost에는 장소명, 만남 시간 유형, 만남 시간을 함께 반환합니다.
                    일정 확정 후에는 companion_schedule.scheduledAt을 우선 반환하고, 미확정 NOW 모집글은 exposureExpiresAt을 meetingAt으로 반환합니다.
                    """,
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
            CompanionMatchAlreadyCanceledException.class
    })
    @ApiResponse(
            responseCode = "200",
            description = "동행 일정 확정 또는 수정 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "CONFIRM_COMPANION_SCHEDULE",
                            value = """
                                    {
                                      "status": 200,
                                      "code": "CONFIRM_COMPANION_SCHEDULE",
                                      "message": "동행 일정이 수정되었어요.",
                                      "data": {
                                        "matchId": 1,
                                        "scheduleId": 1,
                                        "matchStatus": "SCHEDULE_CONFIRMED"
                                      }
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "글 작성자의 동행 일정 확정 및 수정",
            description = "JWT 액세스 토큰으로 인증된 글 작성자가 MATCHED 상태의 일정을 확정하거나 SCHEDULE_CONFIRMED 상태의 일정을 수정합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionMatchScheduleResponse> patchSchedule(
            @Parameter(description = "일정을 확정하거나 수정할 매칭 ID", required = true, example = "1")
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
    @ApiResponse(
            responseCode = "200",
            description = "내 동행 일정 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "READ_COMPANION_SCHEDULE",
                            value = """
                                    {
                                      "status": 200,
                                      "code": "READ_COMPANION_SCHEDULE",
                                      "message": "동행 일정 정보를 조회했어요.",
                                      "data": {
                                        "matchId": 1,
                                        "matchStatus": "MATCHED",
                                        "schedule": {
                                          "place": {
                                            "googlePlaceId": "ChIJxxxxxxxxxxxx",
                                            "name": "Siutat condal",
                                            "address": "Rambla de Catalunya, 16",
                                            "latitude": 41.390205,
                                            "longitude": 2.163548
                                          },
                                          "scheduledAt": "2026-06-18T16:30:00"
                                        },
                                        "openChatUrl": "https://open.kakao.com/o/xxxxxxx",
                                        "userNickname": "루피",
                                        "meetingTimeType": "SCHEDULED",
                                        "currentUserRole": "HOST"
                                      }
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "내 동행 일정 조회",
            description = """
                    JWT 액세스 토큰으로 인증된 사용자가 참여 중인 매칭의 동행 일정 정보를 조회합니다.
                    성공 응답은 MATCHED 또는 SCHEDULE_CONFIRMED 상태만 반환하며, 로그인 사용자의 닉네임, 매칭 내 역할과 모집글의 만남 시간 유형을 함께 반환합니다.
                    MATCHED 상태에서는 모집글의 장소, 시간, 오픈채팅 URL을 기본 일정으로 반환하고, SCHEDULE_CONFIRMED 상태에서는 확정된 일정을 반환합니다.
                    일정 확정 여부는 schedule 존재 여부가 아닌 matchStatus로 판단합니다.
                    meetingTimeType이 UNDECIDED이면 schedule의 장소는 반환되지만 scheduledAt은 null입니다.
                    meetingTimeType이 NOW인 신규 수락 흐름은 일정이 자동 확정되어 SCHEDULE_CONFIRMED 상태로 반환됩니다.
                    기존 DB에 NOW + MATCHED 상태로 남아 있는 데이터는 모집글 장소와 즉시 동행 노출 만료 시간으로 기본 일정을 구성합니다.
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
