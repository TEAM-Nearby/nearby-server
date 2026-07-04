// 온보딩 휴대폰 인증 문자 발송 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.request.ConfirmPhoneVerificationCodeRequest;
import com.sopt.nearby.user.adapter.in.web.dto.request.SendPhoneVerificationCodeRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.ConfirmPhoneVerificationCodeResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.SendPhoneVerificationCodeResponse;
import com.sopt.nearby.user.exception.PhoneVerificationCodeMismatchException;
import com.sopt.nearby.user.exception.PhoneVerificationExpiredException;
import com.sopt.nearby.user.exception.PhoneVerificationNotFoundException;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.oauth2.jwt.Jwt;

@Tag(name = "Onboarding", description = "사용자 온보딩 API")
public interface OnboardingPhoneVerificationApi {

	@Operation(
			summary = "휴대폰 인증 문자 발송",
			description = "인증된 사용자의 휴대폰 번호로 온보딩 인증 문자를 발송합니다.",
			security = @SecurityRequirement(name = "bearerAuth"),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = SendPhoneVerificationCodeRequest.class),
							examples = @ExampleObject(value = """
									{
									  "phoneNumber": "01012345678"
									}
									""")
					)
			)
	)
	@ApiResponse(
			responseCode = "200",
			description = "인증 문자를 발송에 성공했습니다.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "PHONE_VERIFICATION_CODE_SENT",
							  "message": "인증 문자를 발송에 성공했습니다.",
							  "data": {
							    "phoneVerificationId": 10,
							    "expiresIn": 180
							  }
							}
							""")
			)
	)
	@ApiExceptions(PhoneVerificationSendLimitExceededException.class)
	CommonResponse<SendPhoneVerificationCodeResponse> send(
			SendPhoneVerificationCodeRequest request,
			@Parameter(hidden = true) Jwt jwt
	);

	@Operation(
			summary = "휴대폰 인증 번호 확인",
			description = "사용자가 입력한 휴대폰 인증 번호를 확인하고 온보딩 상태를 갱신합니다.",
			security = @SecurityRequirement(name = "bearerAuth"),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = ConfirmPhoneVerificationCodeRequest.class),
							examples = @ExampleObject(value = """
									{
									  "verificationCode": "123456"
									}
									""")
					)
			)
	)
	@ApiResponse(
			responseCode = "200",
			description = "휴대폰 인증에 성공했습니다.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "PHONE_VERIFICATION_CODE_CONFIRMED",
							  "message": "휴대폰 인증에 성공했습니다.",
							  "data": {
							    "phoneVerified": true,
							    "onboardingStatus": "PHONE_VERIFIED"
							  }
							}
							""")
			)
	)
	@ApiExceptions({
			PhoneVerificationCodeMismatchException.class,
			PhoneVerificationNotFoundException.class,
			PhoneVerificationExpiredException.class
	})
	CommonResponse<ConfirmPhoneVerificationCodeResponse> confirm(
			@Parameter(description = "휴대폰 인증 요청 ID", example = "10") Long phoneVerificationId,
			ConfirmPhoneVerificationCodeRequest request,
			@Parameter(hidden = true) Jwt jwt
	);
}
