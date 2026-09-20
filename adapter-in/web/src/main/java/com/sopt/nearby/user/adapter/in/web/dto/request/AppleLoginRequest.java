// 애플 로그인 요청 본문의 ID 토큰과 nonce를 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.AppleLoginCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AppleLoginRequest(
		@Schema(description = "애플 OIDC ID 토큰", example = "apple_oidc_id_token")
		@NotBlank(message = "필수 요청값이 누락되었습니다.")
		String idToken,

		@Schema(description = "애플 로그인 요청에 사용한 nonce", example = "login_request_nonce")
		@NotBlank(message = "필수 요청값이 누락되었습니다.")
		String nonce
) {

	public AppleLoginCommand toCommand() {
		return new AppleLoginCommand(idToken, nonce);
	}
}
