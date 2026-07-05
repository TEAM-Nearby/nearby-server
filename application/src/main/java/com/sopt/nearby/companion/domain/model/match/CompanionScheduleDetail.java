// 내 동행 일정 조회 응답에 사용되는 일정 상세 조회 모델
package com.sopt.nearby.companion.domain.model.match;


import java.math.BigDecimal;
import java.time.LocalDateTime;


public record CompanionScheduleDetail(
        Long matchId,
        CompanionMatchStatus matchStatus,
        Schedule schedule,
        String openChatUrl

) {
    public record Schedule(
            Long scheduleId,
            Place place,
            LocalDateTime scheduledAt
    ) {
    }

    public record Place(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {

    }
}
