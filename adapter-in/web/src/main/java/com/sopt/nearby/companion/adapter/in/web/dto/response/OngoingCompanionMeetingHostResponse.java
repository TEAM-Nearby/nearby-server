// 진행 중인 동행 목록에서 HOST 프로필 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingHostProfile;
import com.sopt.nearby.companion.domain.model.profile.UserGender;

public record OngoingCompanionMeetingHostResponse(
        Long userId,
        String profileImageUrl,
        String nickname,
        UserGender gender
) {

    public static OngoingCompanionMeetingHostResponse from(final OngoingCompanionMeetingHostProfile profile) {
        return new OngoingCompanionMeetingHostResponse(
                profile.userId(),
                profile.profileImageUrl(),
                profile.nickname(),
                profile.gender()
        );
    }
}
