// 본인 프로필 수정 화면에 필요한 현재 값을 전달한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;

public record MyCompanionProfileResult(
        Long profileId, String nickname, UserGender gender, Integer birthYear,
        String intro, String profileImageUrl, List<TravelStyleKeyword> travelStyleKeywords
) {
    public MyCompanionProfileResult {
        travelStyleKeywords = List.copyOf(travelStyleKeywords);
    }

    public static MyCompanionProfileResult from(final CompanionProfile profile,
                                                final List<TravelStyleKeyword> keywords) {
        return new MyCompanionProfileResult(profile.id(), profile.nickname(), profile.gender(), profile.birthYear(),
                profile.intro(), profile.profileImageUrl(), keywords);
    }
}
