// 내 동행 일정 조회 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionScheduleDetailResponse(
        Long matchId,
        CompanionMatchStatus matchStatus,
        CompanionScheduleResponse schedule,
        String openChatUrl
) {
    public static CompanionScheduleDetailResponse from(final CompanionScheduleDetail detail) {
        return new CompanionScheduleDetailResponse(
                detail.matchId(),
                detail.matchStatus(),
                CompanionScheduleResponse.from(detail.schedule()),
                detail.openChatUrl()
        );
    }

    public record CompanionScheduleResponse(
            Long scheduleId,
            PlaceResponse place,
            LocalDateTime scheduledAt
    ) {
        static CompanionScheduleResponse from(final CompanionScheduleDetail.Schedule schedule) {
            if (schedule == null) {
                return null;
            }

            return new CompanionScheduleResponse(
                    schedule.scheduleId(),
                    PlaceResponse.from(schedule.place()),
                    schedule.scheduledAt()
            );
        }
    }

    public record PlaceResponse(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        static PlaceResponse from(final CompanionScheduleDetail.Place place) {
            return new PlaceResponse(
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.latitude(),
                    place.longitude()
            );
        }
    }
}