// 동행 알림 목록 조회와 읽음 처리 HTTP 요청을 처리하는 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionNotificationsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionNotificationPageResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.MarkCompanionNotificationAsReadResponse;
import com.sopt.nearby.companion.application.MarkCompanionNotificationAsReadResult;
import com.sopt.nearby.companion.application.CompanionNotificationPage;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationDirection;
import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.port.in.MarkCompanionNotificationAsReadUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationsUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionNotificationPageUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/companion-requests")
public class CompanionNotificationController implements CompanionNotificationApi {

    private final ReadCompanionNotificationsUseCase readCompanionNotificationsUseCase;
    private final ReadCompanionNotificationPageUseCase readCompanionNotificationPageUseCase;
    private final MarkCompanionNotificationAsReadUseCase markCompanionNotificationAsReadUseCase;

    public CompanionNotificationController(final ReadCompanionNotificationsUseCase readCompanionNotificationsUseCase,
                                           final ReadCompanionNotificationPageUseCase readCompanionNotificationPageUseCase,
                                           final MarkCompanionNotificationAsReadUseCase markCompanionNotificationAsReadUseCase) {
        this.readCompanionNotificationsUseCase = readCompanionNotificationsUseCase;
        this.readCompanionNotificationPageUseCase = readCompanionNotificationPageUseCase;
        this.markCompanionNotificationAsReadUseCase = markCompanionNotificationAsReadUseCase;
    }

    @Override
    @GetMapping("/page")
    public CommonResponse<CompanionNotificationPageResponse> getNotificationPage(
            @RequestParam final String direction,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(required = false) final String cursor,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CompanionNotificationDirection notificationDirection = CompanionNotificationDirection.from(direction);
        CompanionNotificationPage page = readCompanionNotificationPageUseCase.getPage(
                userId,
                notificationDirection,
                size,
                cursor
        );
        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_REQUESTS,
                CompanionNotificationPageResponse.from(notificationDirection, page)
        );
    }

    @Override
    @GetMapping
    public CommonResponse<CompanionNotificationsResponse> getNotifications(
            @RequestParam final String direction,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CompanionNotificationDirection notificationDirection = CompanionNotificationDirection.from(direction);

        List<CompanionNotificationSummary> notifications = readCompanionNotificationsUseCase.getNotifications(
                userId,
                notificationDirection
        );

        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_REQUESTS,
                CompanionNotificationsResponse.from(notificationDirection, notifications)
        );
    }

    @Override
    @PatchMapping("/{notificationId}/read")
    public CommonResponse<MarkCompanionNotificationAsReadResponse> markNotificationAsRead(
            @PathVariable final Long notificationId,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());

        MarkCompanionNotificationAsReadResult result = markCompanionNotificationAsReadUseCase.markAsRead(
                userId,
                notificationId
        );

        return CommonResponse.success(
                CompanionSuccessCode.MARK_COMPANION_NOTIFICATION_AS_READ,
                MarkCompanionNotificationAsReadResponse.from(result)
        );
    }
}
