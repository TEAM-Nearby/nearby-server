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
            description = "JWT 액세스 토큰으로 인증된 사용자의 마이페이지 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<MyPageResponse> getMyPage(
            @Parameter(hidden = true)
            Principal principal
    );
}
