// 동행 푸시 수신 대상 API 문서를 정의한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.request.RegisterCompanionPushEndpointRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionPushEndpointResponse;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionPushEndpointException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionPushEndpoint", description = "동행 푸시 수신 대상 API")
public interface CompanionPushEndpointApi {

    @Operation(
            summary = "푸시 알림 수신 기기 등록",
            description = "인증된 사용자의 앱 설치별 푸시 수신 대상을 멱등하게 등록하거나 갱신합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiExceptions({InvalidCompanionPushEndpointException.class})
    CommonResponse<CompanionPushEndpointResponse> register(
            RegisterCompanionPushEndpointRequest request,
            @Parameter(hidden = true) Principal principal
    );

    @Operation(
            summary = "푸시 알림 수신 기기 해제",
            description = "인증된 사용자에게 연결된 앱 설치의 푸시 수신을 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiExceptions({InvalidCompanionPushEndpointException.class})
    CommonResponse<Void> deactivate(
            String installationId,
            @Parameter(hidden = true) Principal principal
    );
}
