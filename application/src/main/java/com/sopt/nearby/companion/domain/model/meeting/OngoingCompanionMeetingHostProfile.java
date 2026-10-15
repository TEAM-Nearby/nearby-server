// 진행 중인 동행 목록에서 HOST 프로필을 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.meeting;

import com.sopt.nearby.companion.domain.model.profile.UserGender;

public record OngoingCompanionMeetingHostProfile(
        Long userId,
        String profileImageUrl,
        String nickname,
        UserGender gender
) {
}
