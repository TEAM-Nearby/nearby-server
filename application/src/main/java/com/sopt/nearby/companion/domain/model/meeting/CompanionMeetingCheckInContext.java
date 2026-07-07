// 만남 인증에 필요한 만남, 일정, 장소 정보를 담는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionMeetingCheckInContext(
        Long meetingId,
        Long matchId,
        CompanionMeetingStatus meetingStatus,
        Long scheduleId,
        Long placeId,
        LocalDateTime scheduledAt,
        BigDecimal placeLatitude,
        BigDecimal placeLongitude
) {

    public boolean hasConfirmedSchedulePlace() {
        return scheduleId != null
                && placeId != null
                && scheduledAt != null
                && placeLatitude != null
                && placeLongitude != null;
    }
}
