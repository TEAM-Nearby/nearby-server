// 동행 프로필 등록 요청 본문을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.RegisterCompanionProfileCommand;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.List;

public record RegisterCompanionProfileRequest(
		@Schema(description = "동행 프로필 닉네임", example = "여행친구")
		@NotNull(message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		@Size(min = 1, max = 15, message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		String nickname,

		@Schema(description = "성별", example = "FEMALE")
		@NotNull(message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		UserGender gender,

		@Schema(description = "한줄소개", example = "혼자 여행도 같이 여행도 좋아해요")
		@Size(max = 50, message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		String intro,

		@Schema(description = "S3 업로드 후 받은 이미지 URL")
		@Size(max = 255, message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		String profileImageUrl,

		@Schema(description = "여행 스타일 키워드 목록", example = "[\"EXTROVERTED\", \"CAFE_TOUR\"]")
		@NotEmpty(message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
		List<TravelStyleKeyword> travelStyleKeywords
) {

	public RegisterCompanionProfileCommand toCommand(final Long userId) {
		return new RegisterCompanionProfileCommand(
				userId,
				nickname,
				gender,
				intro,
				profileImageUrl,
				travelStyleKeywords
		);
	}

	@AssertTrue(message = "필수값 누락되었거나 형식에 오류가 발생했습니다.")
	@Schema(hidden = true)
	public boolean isTravelStyleKeywordsDistinct() {
		return travelStyleKeywords == null || new HashSet<>(travelStyleKeywords).size() == travelStyleKeywords.size();
	}
}
