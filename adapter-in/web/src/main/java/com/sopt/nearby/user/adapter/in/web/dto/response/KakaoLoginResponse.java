// 카카오 로그인 성공 시 Nearby 토큰과 사용자 상태를 반환하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.KakaoLoginResult;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record KakaoLoginResponse(
		@Schema(description = "Nearby API 호출용 액세스 토큰")
		String accessToken,

		@Schema(description = "액세스 토큰 재발급용 리프레시 토큰")
		String refreshToken,

		@Schema(description = "토큰 타입", example = "Bearer")
		String tokenType,

		@Schema(description = "액세스 토큰 만료까지 남은 초", example = "3600")
		long accessTokenExpiresIn,

		@Schema(description = "리프레시 토큰 만료까지 남은 초", example = "1209600")
		long refreshTokenExpiresIn,

		@Schema(description = "Nearby 사용자 ID", example = "1")
		Long userId,

		@Schema(description = "사용자 온보딩 상태", allowableValues = {"STARTED", "PHONE_VERIFIED", "COMPLETED"})
		String onboardingStatus
) {

	public static KakaoLoginResponse from(final KakaoLoginResult result) {
		return new KakaoLoginResponse(
				result.accessToken(),
				result.refreshToken(),
				result.tokenType(),
				result.accessTokenExpiresIn(),
				result.refreshTokenExpiresIn(),
				result.userId(),
				toApiOnboardingStatus(result.onboardingStatus())
		);
	}

	private static String toApiOnboardingStatus(final UserOnboardingStatus status) {
		return switch (status) {
			case PHONE_VERIFIED -> "PHONE_VERIFIED";
			case COMPLETED, COMPANION_PROFILE_COMPLETED, COMPANION_PROFILE_SKIPPED -> "COMPLETED";
			default -> "STARTED";
		};
	}
}
