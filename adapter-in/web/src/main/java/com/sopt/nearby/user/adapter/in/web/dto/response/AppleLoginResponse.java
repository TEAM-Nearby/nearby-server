// 애플 로그인 성공 시 Nearby 토큰과 사용자 상태를 반환하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.AppleLoginResult;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AppleLoginResponse(
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

	public static AppleLoginResponse from(final AppleLoginResult result) {
		return new AppleLoginResponse(
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
