// 진행 중인 동행 목록의 단일 만남 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.time.LocalDateTime;

public record OngoingCompanionMeetingSummary(
        Long meetingId,
        Long matchId,
        OngoingCompanionMeetingHostProfile companion,
        String placeName,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        boolean checkedIn,
        CompanionMeetingStatus meetingStatus
) {
}
