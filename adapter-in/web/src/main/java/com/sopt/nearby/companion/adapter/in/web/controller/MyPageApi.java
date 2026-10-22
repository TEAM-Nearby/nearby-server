// 마이페이지 조회 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.MyPageResponse;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "MyPage", description = "마이페이지 API")
public interface MyPageApi {

    @ApiExceptions({
            CompanionProfileNotFoundException.class
    })
    @Operation(
            summary = "마이페이지 조회",
            description = "인증된 사용자의 마이페이지를 조회합니다. profileId는 본인의 동행 프로필 ID이고, "
                    + "intro는 저장된 한 줄 소개이며 소개가 없으면 null입니다. "
                    + "프로필 등록을 건너뛴 경우 hasCompanionProfile은 false이고, "
                    + "profileId와 intro를 포함한 프로필 정보는 null, 활동 목록은 빈 배열, 집계는 0입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<MyPageResponse> getMyPage(
            @Parameter(hidden = true)
            Principal principal
    );
}
