// 로그아웃 요청 본문의 리프레시 토큰을 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.LogoutUserCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record LogoutRequest(
		@Schema(description = "Nearby 서버 로그인 시 발급받은 Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
		String refreshToken
) {

	public LogoutUserCommand toCommand(final Long userId) {
		return new LogoutUserCommand(userId, refreshToken);
	}
}
