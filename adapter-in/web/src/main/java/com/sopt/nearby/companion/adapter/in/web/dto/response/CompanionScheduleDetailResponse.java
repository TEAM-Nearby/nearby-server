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
        @Schema(
                description = "매칭 상태. MATCHED는 기본 일정, SCHEDULE_CONFIRMED는 확정 일정",
                allowableValues = {"MATCHED", "SCHEDULE_CONFIRMED"},
                example = "MATCHED",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        CompanionMatchStatus matchStatus,
        @Schema(description = "화면 표시 및 수정에 사용하는 일정 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        CompanionScheduleResponse schedule,
        @Schema(description = "카카오톡 오픈채팅 URL", requiredMode = Schema.RequiredMode.REQUIRED)
        String openChatUrl,
        @Schema(description = "로그인한 사용자의 닉네임", requiredMode = Schema.RequiredMode.REQUIRED)
        String userNickname,
        @Schema(
                description = "모집글의 만남 시간 유형",
                allowableValues = {"NOW", "SCHEDULED", "UNDECIDED"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
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
            @Schema(description = "만남 날짜 및 시간. UNDECIDED이면 null", nullable = true)
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
