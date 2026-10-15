// 진행 중인 동행 목록의 단일 만남 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record OngoingCompanionMeetingResponse(
        Long meetingId,
        Long matchId,
        OngoingCompanionMeetingHostResponse companion,
        String placeName,
        CompanionCity city,
        String timeZoneId,
        OffsetDateTime currentLocalTime,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        boolean isCheckedIn,
        CompanionMeetingStatus meetingStatus,
        CompanionMeetingProgressStatus progressStatus
) {

    public static OngoingCompanionMeetingResponse from(final OngoingCompanionMeetingSummary summary) {
        return new OngoingCompanionMeetingResponse(
                summary.meetingId(),
                summary.matchId(),
                OngoingCompanionMeetingHostResponse.from(summary.companion()),
                summary.placeName(),
                summary.city(),
                summary.city() == null ? null : summary.city().zoneId().getId(),
                summary.currentLocalTime() == null ? null : summary.currentLocalTime().toOffsetDateTime(),
                summary.meetingAt(),
                summary.meetingTimeType(),
                summary.checkedIn(),
                summary.meetingStatus(),
                summary.progressStatus()
        );
    }
}
