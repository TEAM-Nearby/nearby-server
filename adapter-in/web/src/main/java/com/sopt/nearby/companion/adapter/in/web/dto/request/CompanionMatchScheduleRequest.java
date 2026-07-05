// 동행 일정 확정 요청 본문을 유스케이스 명령으로 변환하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.ConfirmCompanionScheduleCommand;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionMatchScheduleRequest(
        LocalDateTime scheduledAt,
        PlaceRequest place,
        String openChatUrl
) {
    public ConfirmCompanionScheduleCommand toCommand(final Long matchId, final Long requesterUserId) {
        return new ConfirmCompanionScheduleCommand(
                matchId,
                requesterUserId,
                scheduledAt,
                place == null ? null : place.toCommandPlace(),
                openChatUrl
        );
    }

    public record PlaceRequest(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        ConfirmCompanionScheduleCommand.Place toCommandPlace() {
            return new ConfirmCompanionScheduleCommand.Place(
                    googlePlaceId,
                    name,
                    address,
                    latitude,
                    longitude
            );
        }
    }
}