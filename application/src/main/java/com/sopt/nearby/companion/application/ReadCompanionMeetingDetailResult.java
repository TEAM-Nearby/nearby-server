// 진행 중인 동행 상세 조회 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;

public record ReadCompanionMeetingDetailResult(
        Long meetingId,
        MatchParticipantRole currentUserRole,
        Long hostId,
        UserGender hostGender,
        String hostProfileImageUrl,
        String hostNickname,
        boolean hostCheckedIn,
        String placeName,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        CompanionMeetingStatus meetingStatus,
        boolean currentUserCheckedIn,
        boolean canCancelMeeting
) {

    public static ReadCompanionMeetingDetailResult from(final CompanionMeetingDetail detail) {
        return new ReadCompanionMeetingDetailResult(
                detail.meetingId(),
                detail.currentUserRole(),
                detail.hostId(),
                detail.hostGender(),
                detail.hostProfileImageUrl(),
                detail.hostNickname(),
                detail.hostCheckedIn(),
                detail.placeName(),
                detail.meetingAt(),
                detail.meetingTimeType(),
                detail.meetingStatus(),
                detail.currentUserCheckedIn(),
                detail.meetingStatus() == CompanionMeetingStatus.ONGOING
        );
    }
}
