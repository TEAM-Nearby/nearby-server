// 진행 중인 동행 상세 조회에 필요한 만남 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;

public record CompanionMeetingDetail(
        Long meetingId,
        MatchParticipantRole currentUserRole,
        Long hostId,
        UserGender hostGender,
        String hostProfileImageUrl,
        String hostNickname,
        boolean hostCheckedIn,
        String placeName,
        String placeAddress,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        CompanionMeetingStatus meetingStatus,
        boolean currentUserCheckedIn
) {

    public CompanionMeetingDetail(
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
            final boolean currentUserCheckedIn
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
                meetingAt,
                meetingTimeType,
                meetingStatus,
                currentUserCheckedIn
        );
    }
}
