// 토큰 재발급에 사용할 리프레시 토큰을 받는 요청 DTO
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.RefreshTokenCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenRequest(
		@Schema(description = "Nearby 서버 로그인 시 발급받은 Refresh Token", example = "refresh_token_value")
		String refreshToken
) {

	public RefreshTokenCommand toCommand() {
		return new RefreshTokenCommand(refreshToken);
	}
}
