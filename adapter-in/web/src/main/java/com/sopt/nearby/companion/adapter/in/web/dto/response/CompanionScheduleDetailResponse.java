// 내 동행 일정 조회 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionScheduleDetailResponse(
        Long matchId,
        CompanionMatchStatus matchStatus,
        CompanionScheduleResponse schedule,
        String openChatUrl,
        String userNickname,
        CompanionPostMeetingTimeType meetingTimeType,
        @Schema(
                description = "로그인한 사용자의 해당 매칭 내 역할",
                allowableValues = {"HOST", "GUEST"},
                example = "HOST",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        MatchParticipantRole currentUserRole
) {
    public static CompanionScheduleDetailResponse from(final CompanionScheduleDetail detail) {
        return new CompanionScheduleDetailResponse(
                detail.matchId(),
                detail.matchStatus(),
                CompanionScheduleResponse.from(detail.schedule()),
                detail.openChatUrl(),
                detail.userNickname(),
                detail.meetingTimeType(),
                detail.currentUserRole()
        );
    }

    public record CompanionScheduleResponse(
            PlaceResponse place,
            LocalDateTime scheduledAt
    ) {
        static CompanionScheduleResponse from(final CompanionScheduleDetail.Schedule schedule) {
            if (schedule == null) {
                return null;
            }

            return new CompanionScheduleResponse(
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
