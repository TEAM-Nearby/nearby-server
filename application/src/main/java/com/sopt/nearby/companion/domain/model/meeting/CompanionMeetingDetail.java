// 진행 중인 동행 상세 조회에 필요한 만남 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
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
        LocalDateTime meetingAt,
        CompanionMeetingStatus meetingStatus,
        boolean currentUserCheckedIn
) {
}
