// 동행 일정 확정 요청 값을 애플리케이션 계층으로 전달하는 명령 객체
package com.sopt.nearby.companion.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ConfirmCompanionScheduleCommand(
        Long matchId,
        Long requesterUserId,
        LocalDateTime scheduledAt,
        Place place,
        String openChatUrl
) {
    public record Place(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }
}
