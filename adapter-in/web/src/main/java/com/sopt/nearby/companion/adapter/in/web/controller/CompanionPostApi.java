// 동행 모집글 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionPostRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPostDetailResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreatedCompanionPostResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPostsResponse;
import com.sopt.nearby.companion.domain.exception.CompanionPostExpiredException;
import com.sopt.nearby.companion.domain.exception.CompanionPostNotFoundException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostCreateRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPostSearchRequestException;
import com.sopt.nearby.companion.domain.exception.InvalidOpenChatUrlException;
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

@Tag(name = "CompanionPost", description = "동행 모집글 API")
public interface CompanionPostApi {

    @ApiExceptions({
            InvalidCompanionPostSearchRequestException.class
    })
    @ApiResponse(
            responseCode = "403",
            description = "온보딩 과정이 완료되지 않았습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "ONBOARDING_REQUIRED",
                            value = """
                                    {
                                      "status": 403,
                                      "code": "ONBOARDING_REQUIRED",
                                      "message": "온보딩 과정이 완료되지 않았습니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "동행 모집글 목록 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 현재 위치와 필터 조건에 맞는 동행 모집글을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionPostsResponse> getPosts(
            @Parameter(description = "사용자 현재 위치 위도", required = true, example = "37.56650000")
            String latitude,
            @Parameter(description = "사용자 현재 위치 경도", required = true, example = "126.97800000")
            String longitude,
            @Parameter(description = "조회 반경, 미터 단위", example = "1000")
            String radiusMeters,
            @Parameter(description = "장소 카테고리 필터", example = "ALL")
            String placeCategory,
            @Parameter(description = "특정 장소 ID 필터", example = "20")
            String placeId,
            @Parameter(description = "목록 정렬 기준", example = "LATEST")
            String sort,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            CompanionPostNotFoundException.class,
            CompanionPostExpiredException.class
    })
    @ApiResponse(
            responseCode = "403",
            description = "온보딩 과정이 완료되지 않았습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "ONBOARDING_REQUIRED",
                            value = """
                                    {
                                      "status": 403,
                                      "code": "ONBOARDING_REQUIRED",
                                      "message": "온보딩 과정이 완료되지 않았습니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "동행 모집글 상세 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 동행 모집글 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionPostDetailResponse> getPost(
            @Parameter(description = "조회할 동행 모집 글 ID", required = true, example = "101")
            Long postId,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidCompanionPostCreateRequestException.class,
            InvalidOpenChatUrlException.class
    })
    @ApiResponse(
            responseCode = "403",
            description = "온보딩 과정이 완료되지 않았습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            name = "ONBOARDING_REQUIRED",
                            value = """
                                    {
                                      "status": 403,
                                      "code": "ONBOARDING_REQUIRED",
                                      "message": "온보딩 과정이 완료되지 않았습니다.",
                                      "data": null
                                    }
                                    """
                    )
            )
    )
    @Operation(
            summary = "동행 모집글 작성",
            description = "JWT 액세스 토큰으로 인증된 사용자가 동행 모집글을 작성합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CreatedCompanionPostResponse> createPost(
            CreateCompanionPostRequest request,
            @Parameter(hidden = true)
            Principal principal
    );
}
