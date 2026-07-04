// 휴대폰 인증 번호 확인 결과를 API 응답으로 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmPhoneVerificationCodeResponse(
		@Schema(description = "휴대폰 인증 완료 여부", example = "true")
		boolean phoneVerified,
		@Schema(description = "사용자 온보딩 상태", example = "PHONE_VERIFIED")
		String onboardingStatus
) {

	public static ConfirmPhoneVerificationCodeResponse from(final ConfirmPhoneVerificationCodeResult result) {
		return new ConfirmPhoneVerificationCodeResponse(result.phoneVerified(), result.onboardingStatus().name());
	}
}
