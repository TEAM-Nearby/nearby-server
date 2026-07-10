// 토큰 재발급 결과를 반환하는 응답 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.RefreshTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenResponse(
		@Schema(description = "API 요청 인증에 사용할 액세스 토큰")
		String accessToken,
		@Schema(description = "다음 액세스 토큰 재발급에 사용할 리프레시 토큰")
		String refreshToken,
		@Schema(description = "토큰 인증 타입", example = "Bearer")
		String tokenType,
		@Schema(description = "액세스 토큰 만료까지 남은 시간(초)", example = "3600")
		long accessTokenExpiresIn,
		@Schema(description = "리프레시 토큰 만료까지 남은 시간(초)", example = "1209600")
		long refreshTokenExpiresIn
) {

	public static RefreshTokenResponse from(final RefreshTokenResult result) {
		return new RefreshTokenResponse(
				result.accessToken(),
				result.refreshToken(),
				result.tokenType(),
				result.accessTokenExpiresIn(),
				result.refreshTokenExpiresIn()
		);
	}
}
