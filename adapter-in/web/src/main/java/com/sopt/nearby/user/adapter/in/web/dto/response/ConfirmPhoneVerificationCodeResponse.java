// 휴대폰 인증 번호 확인 결과를 API 응답으로 표현하는 DTO
package com.sopt.nearby.user.adapter.in.web.dto.response;

import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record ConfirmPhoneVerificationCodeResponse(
		@Schema(description = "휴대폰 인증 완료 여부", example = "true")
		boolean phoneVerified,
		@Schema(
				description = "인증 후 온보딩 상태. 이미 등록하거나 건너뛴 사용자는 COMPLETED를 반환합니다.",
				example = "PHONE_VERIFIED",
				allowableValues = {"PHONE_VERIFIED", "COMPLETED"}
		)
		String onboardingStatus
) {

	public static ConfirmPhoneVerificationCodeResponse from(final ConfirmPhoneVerificationCodeResult result) {
		return new ConfirmPhoneVerificationCodeResponse(
				result.phoneVerified(), OnboardingStatusMapper.toApiStatus(result.onboardingStatus()));
	}
}
