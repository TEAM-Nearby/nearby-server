// 동행 프로필 등록 결과를 담는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;

public record RegisteredCompanionProfileResult(
		Long profileId,
		String nickname,
		UserGender gender,
		String intro,
		String profileImageUrl,
		List<TravelStyleKeyword> travelStyleKeywords,
		String onboardingStatus
) {
}

