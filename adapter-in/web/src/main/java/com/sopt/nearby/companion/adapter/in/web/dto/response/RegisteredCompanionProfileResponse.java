// 동행 프로필 등록 결과를 API 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.RegisteredCompanionProfileResult;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record RegisteredCompanionProfileResponse(
		@Schema(description = "동행 프로필 ID", example = "5")
		Long profileId,
		@Schema(description = "등록된 닉네임", example = "여행친구")
		String nickname,
		@Schema(description = "등록된 성별", example = "FEMALE")
		String gender,
		@Schema(description = "등록된 한줄소개")
		String intro,
		@Schema(description = "등록된 프로필 이미지 URL")
		String profileImageUrl,
		@Schema(description = "등록된 여행 스타일 키워드 목록")
		List<String> travelStyleKeywords,
		@Schema(description = "사용자 온보딩 상태", example = "COMPLETED")
		String onboardingStatus
) {

	public static RegisteredCompanionProfileResponse from(final RegisteredCompanionProfileResult result) {
		return new RegisteredCompanionProfileResponse(
				result.profileId(),
				result.nickname(),
				result.gender().name(),
				result.intro(),
				result.profileImageUrl(),
				result.travelStyleKeywords().stream()
						.map(TravelStyleKeyword::name)
						.toList(),
				result.onboardingStatus()
		);
	}
}

