// 본인 동행 프로필 조회와 수정 API 계약을 문서화한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.UpdateMyCompanionProfileRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MyCompanionProfileResponse;
import com.sopt.nearby.companion.domain.exception.CompanionProfileRequiredException;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.companion.domain.exception.InactiveCompanionProfileException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionProfileUpdateException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "MyPage", description = "마이페이지 API")
@SecurityRequirement(name = "bearerAuth")
public interface MyCompanionProfileApi {
    @Operation(summary = "내 동행 프로필 조회",
            description = "인증된 사용자 본인의 수정 화면 초기값을 조회합니다. 프로필 미등록 시 "
                    + "COMPANION_PROFILE_REQUIRED를 반환하며 기존 온보딩 프로필 등록 API를 사용해야 합니다.")
    @ApiResponse(responseCode = "200", description = "본인 프로필 정보를 반환합니다.")
    @ApiResponse(responseCode = "403", description = "온보딩 미완료, 또는 프로필 미등록·비활성 상태입니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class),
                    examples = @ExampleObject(name = "ONBOARDING_REQUIRED", value = """
                            {"status":403,"code":"ONBOARDING_REQUIRED","message":"온보딩 과정이 완료되지 않았습니다.","data":null}
                            """)))
    @ApiResponse(responseCode = "404", description = "인증 정보에 해당하는 사용자가 없습니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class),
                    examples = @ExampleObject(name = "USER_NOT_FOUND", value = """
                            {"status":404,"code":"USER_NOT_FOUND","message":"사용자를 찾을 수 없습니다.","data":null}
                            """)))
    @ApiExceptions({CompanionProfileRequiredException.class, InactiveCompanionProfileException.class})
    CommonResponse<MyCompanionProfileResponse> getProfile(@Parameter(hidden = true) Principal principal);

    @Operation(summary = "내 동행 프로필 수정",
            description = "닉네임·한줄소개·이미지·여행 스타일 전체를 교체합니다. 닉네임과 스타일은 필수이며, "
                    + "소개와 이미지는 생략/null/빈 문자열/공백이면 삭제됩니다. 성별·출생연도·활동 정보·온보딩 상태는 유지합니다. "
                    + "이미지는 기존 POST /api/onboarding/profile-images/presigned-url로 발급받아 업로드한 URL을 전달합니다. "
                    + "본인의 기존 닉네임은 허용하며 타인과 중복되면 DUPLICATE_NICKNAME을 반환합니다. "
                    + "미등록 프로필은 기존 온보딩 등록 API를 사용해야 하며 비활성 프로필은 수정할 수 없습니다.")
    @ApiResponse(responseCode = "200", description = "저장된 프로필 정보를 반환합니다.")
    @ApiResponse(responseCode = "403", description = "온보딩 미완료, 또는 프로필 미등록·비활성 상태입니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class),
                    examples = @ExampleObject(name = "ONBOARDING_REQUIRED", value = """
                            {"status":403,"code":"ONBOARDING_REQUIRED","message":"온보딩 과정이 완료되지 않았습니다.","data":null}
                            """)))
    @ApiResponse(responseCode = "404", description = "인증 정보에 해당하는 사용자가 없습니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class),
                    examples = @ExampleObject(name = "USER_NOT_FOUND", value = """
                            {"status":404,"code":"USER_NOT_FOUND","message":"사용자를 찾을 수 없습니다.","data":null}
                            """)))
    @ApiExceptions({InvalidCompanionProfileUpdateException.class, DuplicateNicknameException.class,
            CompanionProfileRequiredException.class, InactiveCompanionProfileException.class})
    CommonResponse<MyCompanionProfileResponse> updateProfile(UpdateMyCompanionProfileRequest request,
                                                            @Parameter(hidden = true) Principal principal);
}
