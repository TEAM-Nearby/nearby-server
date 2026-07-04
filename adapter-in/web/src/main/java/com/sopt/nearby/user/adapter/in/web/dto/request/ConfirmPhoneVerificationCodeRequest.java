// 휴대폰 인증 번호 확인 요청 본문의 인증 번호를 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeCommand;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmPhoneVerificationCodeRequest(
		@Schema(description = "사용자가 입력한 인증 번호", example = "123456")
		String verificationCode
) {

	public ConfirmPhoneVerificationCodeCommand toCommand(final Long userId, final Long phoneVerificationId) {
		return new ConfirmPhoneVerificationCodeCommand(userId, phoneVerificationId, verificationCode);
	}
}
