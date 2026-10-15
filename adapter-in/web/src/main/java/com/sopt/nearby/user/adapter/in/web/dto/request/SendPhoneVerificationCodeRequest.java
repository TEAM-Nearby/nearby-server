// 휴대폰 인증 문자 발송 요청 본문의 전화번호를 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.request;

import com.sopt.nearby.user.application.SendPhoneVerificationCodeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendPhoneVerificationCodeRequest(
		@Schema(description = "인증 문자를 받을 휴대폰 번호", example = "01012345678")
		@NotBlank(message = "전화번호 형식이 올바르지 않습니다.")
		@Pattern(regexp = "\\d{11}", message = "전화번호 형식이 올바르지 않습니다.")
		String phoneNumber
) {

	public SendPhoneVerificationCodeCommand toCommand(final Long userId) {
		return new SendPhoneVerificationCodeCommand(userId, phoneNumber);
	}
}
