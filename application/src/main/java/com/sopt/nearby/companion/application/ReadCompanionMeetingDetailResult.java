// 진행 중인 동행 상세 조회 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record ReadCompanionMeetingDetailResult(
        Long meetingId,
        MatchParticipantRole currentUserRole,
        Long hostId,
        UserGender hostGender,
        String hostProfileImageUrl,
        String hostNickname,
        boolean hostCheckedIn,
        String placeName,
        CompanionCity city,
        ZonedDateTime currentLocalTime,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        CompanionMeetingStatus meetingStatus,
        boolean currentUserCheckedIn,
        boolean canCancelMeeting
) {

    public static ReadCompanionMeetingDetailResult from(
            final CompanionMeetingDetail detail,
            final CompanionCity city,
            final ZonedDateTime currentLocalTime
    ) {
        return new ReadCompanionMeetingDetailResult(
                detail.meetingId(),
                detail.currentUserRole(),
                detail.hostId(),
                detail.hostGender(),
                detail.hostProfileImageUrl(),
                detail.hostNickname(),
                detail.hostCheckedIn(),
                detail.placeName(),
                city,
                currentLocalTime,
                detail.meetingAt(),
                detail.meetingTimeType(),
                detail.meetingStatus(),
                detail.currentUserCheckedIn(),
                detail.meetingStatus() == CompanionMeetingStatus.ONGOING
        );
    }

    public ReadCompanionMeetingDetailResult(
            final Long meetingId,
            final MatchParticipantRole currentUserRole,
            final Long hostId,
            final UserGender hostGender,
            final String hostProfileImageUrl,
            final String hostNickname,
            final boolean hostCheckedIn,
            final String placeName,
            final LocalDateTime meetingAt,
            final CompanionPostMeetingTimeType meetingTimeType,
            final CompanionMeetingStatus meetingStatus,
            final boolean currentUserCheckedIn,
            final boolean canCancelMeeting
    ) {
        this(
                meetingId,
                currentUserRole,
                hostId,
                hostGender,
                hostProfileImageUrl,
                hostNickname,
                hostCheckedIn,
                placeName,
                null,
                null,
                meetingAt,
                meetingTimeType,
                meetingStatus,
                currentUserCheckedIn,
                canCancelMeeting
        );
    }
}
