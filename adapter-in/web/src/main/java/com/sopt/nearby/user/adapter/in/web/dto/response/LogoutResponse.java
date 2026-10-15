// 로그아웃 성공 여부를 반환하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.LogoutUserResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record LogoutResponse(
		@Schema(description = "로그아웃 처리 여부", example = "true")
		boolean loggedOut
) {

	public static LogoutResponse from(final LogoutUserResult result) {
		return new LogoutResponse(result.loggedOut());
	}
}
