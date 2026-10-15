// 내 동행 일정 조회 응답에 사용되는 일정 상세 조회 모델
package com.sopt.nearby.companion.domain.model.match;


import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;


public record CompanionScheduleDetail(
        Long matchId,
        CompanionMatchStatus matchStatus,
        CompanionCity city,
        ZonedDateTime currentLocalTime,
        Schedule schedule,
        String openChatUrl,
        String userNickname,
        CompanionPostMeetingTimeType meetingTimeType,
        MatchParticipantRole currentUserRole

) {
    public record Schedule(
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
