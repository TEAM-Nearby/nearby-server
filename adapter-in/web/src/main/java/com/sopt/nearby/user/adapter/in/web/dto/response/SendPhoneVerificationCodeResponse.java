// 휴대폰 인증 문자 발송 성공 응답을 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.SendPhoneVerificationCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record SendPhoneVerificationCodeResponse(
		@Schema(description = "휴대폰 인증 요청 ID", example = "10")
		Long phoneVerificationId,

		@Schema(description = "인증 코드 만료까지 남은 초", example = "180")
		int expiresIn
) {

	public static SendPhoneVerificationCodeResponse from(final SendPhoneVerificationCodeResult result) {
		return new SendPhoneVerificationCodeResponse(result.phoneVerificationId(), result.expiresIn());
	}
}
