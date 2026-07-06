// 동행 알림 목록 조회와 읽음 처리 API 문서를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionNotificationsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MarkCompanionNotificationAsReadResponse;
import com.sopt.nearby.companion.domain.exception.CompanionNotificationNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionNotificationException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationDirectionException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionNotificationIdException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "CompanionNotification", description = "동행 알림 API")
public interface CompanionNotificationApi {

    @ApiExceptions({
            InvalidCompanionNotificationDirectionException.class
    })
    @Operation(
            summary = "동행 알림 목록 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자의 보낸 요청 또는 받은 요청 알림 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<CompanionNotificationsResponse> getNotifications(
            @Parameter(description = "조회할 요청 방향", required = true, example = "SENT")
            String direction,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidCompanionNotificationIdException.class,
            ForbiddenCompanionNotificationException.class,
            CompanionNotificationNotFoundException.class
    })
    @Operation(
            summary = "동행 알림 읽음 처리",
            description = "JWT 액세스 토큰으로 인증된 사용자의 동행 알림을 읽음 처리합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<MarkCompanionNotificationAsReadResponse> markNotificationAsRead(
            @Parameter(description = "읽음 처리할 동행 알림 ID", required = true, example = "1")
            Long notificationId,
            @Parameter(hidden = true)
            Principal principal
    );
}

