// 온보딩 휴대폰 인증 문자 발송 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.request.SendPhoneVerificationCodeRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.SendPhoneVerificationCodeResponse;
import com.sopt.nearby.user.adapter.in.web.response.OnboardingSuccessCode;
import com.sopt.nearby.user.application.SendPhoneVerificationCodeResult;
import com.sopt.nearby.user.port.in.SendPhoneVerificationCodeUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/onboarding/phone-verifications")
public class OnboardingPhoneVerificationController implements OnboardingPhoneVerificationApi {

	private final SendPhoneVerificationCodeUseCase sendPhoneVerificationCodeUseCase;

	public OnboardingPhoneVerificationController(
			final SendPhoneVerificationCodeUseCase sendPhoneVerificationCodeUseCase
	) {
		this.sendPhoneVerificationCodeUseCase = sendPhoneVerificationCodeUseCase;
	}

	@Override
	@PostMapping
	public CommonResponse<SendPhoneVerificationCodeResponse> send(
			@Valid @RequestBody final SendPhoneVerificationCodeRequest request,
			@AuthenticationPrincipal final Jwt jwt
	) {
		SendPhoneVerificationCodeResult result = sendPhoneVerificationCodeUseCase.send(
				request.toCommand(Long.valueOf(jwt.getSubject()))
		);
		return CommonResponse.success(
				OnboardingSuccessCode.PHONE_VERIFICATION_CODE_SENT,
				SendPhoneVerificationCodeResponse.from(result)
		);
	}
}
