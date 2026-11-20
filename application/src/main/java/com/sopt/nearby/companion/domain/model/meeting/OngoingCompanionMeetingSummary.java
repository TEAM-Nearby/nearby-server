// 진행 중인 동행 목록의 단일 만남 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record OngoingCompanionMeetingSummary(
        Long meetingId,
        Long matchId,
        OngoingCompanionMeetingHostProfile companion,
        String placeName,
        String placeAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        CompanionCity city,
        ZonedDateTime currentLocalTime,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        boolean checkedIn,
        CompanionMeetingStatus meetingStatus,
        CompanionMeetingProgressStatus progressStatus
) {

    public OngoingCompanionMeetingSummary(
            final Long meetingId,
            final Long matchId,
            final OngoingCompanionMeetingHostProfile companion,
            final String placeName,
            final String placeAddress,
            final CompanionCity city,
            final ZonedDateTime currentLocalTime,
            final LocalDateTime meetingAt,
            final CompanionPostMeetingTimeType meetingTimeType,
            final boolean checkedIn,
            final CompanionMeetingStatus meetingStatus,
            final CompanionMeetingProgressStatus progressStatus
    ) {
        this(meetingId, matchId, companion, placeName, placeAddress, null, null, city,
                currentLocalTime, meetingAt, meetingTimeType, checkedIn, meetingStatus, progressStatus);
    }

    public OngoingCompanionMeetingSummary(
            final Long meetingId,
            final Long matchId,
            final OngoingCompanionMeetingHostProfile companion,
            final String placeName,
            final LocalDateTime meetingAt,
            final CompanionPostMeetingTimeType meetingTimeType,
            final boolean checkedIn,
            final CompanionMeetingStatus meetingStatus
    ) {
        this(
                meetingId,
                matchId,
                companion,
                placeName,
                null,
                null,
                null,
                null,
                null,
                meetingAt,
                meetingTimeType,
                checkedIn,
                meetingStatus,
                CompanionMeetingProgressStatus.ONGOING
        );
    }

	public OngoingCompanionMeetingSummary(
			final Long meetingId,
			final Long matchId,
			final OngoingCompanionMeetingHostProfile companion,
			final String placeName,
			final LocalDateTime meetingAt,
			final CompanionPostMeetingTimeType meetingTimeType,
			final boolean checkedIn,
			final CompanionMeetingStatus meetingStatus,
			final CompanionMeetingProgressStatus progressStatus
	) {
		this(
				meetingId,
				matchId,
				companion,
				placeName,
				null,
				null,
				null,
				null,
				null,
				meetingAt,
				meetingTimeType,
				checkedIn,
				meetingStatus,
				progressStatus
		);
	}
}
