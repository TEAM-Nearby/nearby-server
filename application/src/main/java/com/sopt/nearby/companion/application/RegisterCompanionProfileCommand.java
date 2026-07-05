// 동행 프로필 등록 요청 값을 담는 커맨드
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.util.List;

public record RegisterCompanionProfileCommand(
		Long userId,
		String nickname,
		UserGender gender,
		String intro,
		String profileImageUrl,
		List<TravelStyleKeyword> travelStyleKeywords
) {
}

