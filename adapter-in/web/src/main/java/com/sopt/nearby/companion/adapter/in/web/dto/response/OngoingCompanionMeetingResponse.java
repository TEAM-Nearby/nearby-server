// 진행 중인 동행 목록의 단일 만남 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import java.time.LocalDateTime;

public record OngoingCompanionMeetingResponse(
        Long meetingId,
        Long matchId,
        OngoingCompanionMeetingHostResponse companion,
        String placeName,
        LocalDateTime meetingAt,
        boolean isCheckedIn,
        CompanionMeetingStatus meetingStatus
) {

    public static OngoingCompanionMeetingResponse from(final OngoingCompanionMeetingSummary summary) {
        return new OngoingCompanionMeetingResponse(
                summary.meetingId(),
                summary.matchId(),
                OngoingCompanionMeetingHostResponse.from(summary.companion()),
                summary.placeName(),
                summary.meetingAt(),
                summary.checkedIn(),
                summary.meetingStatus()
        );
    }
}
