// 애플 로그인 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.request.AppleLoginRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.AppleLoginResponse;
import com.sopt.nearby.user.exception.AppleClientIdNotConfiguredException;
import com.sopt.nearby.user.exception.AppleIdTokenAudienceMismatchException;
import com.sopt.nearby.user.exception.AppleIdTokenNonceMismatchException;
import com.sopt.nearby.user.exception.AppleIdTokenSubjectMissingException;
import com.sopt.nearby.user.exception.AppleIdTokenVerificationFailedException;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Social Login", description = "소셜 로그인 API")
public interface AppleLoginApi {

	@Operation(
			summary = "애플 로그인",
			description = "애플 SDK에서 받은 ID 토큰, nonce, 인증 코드로 Nearby 토큰을 발급합니다.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = AppleLoginRequest.class),
							examples = @ExampleObject(value = """
									{
									  "idToken": "apple_oidc_id_token",
									  "nonce": "login_request_nonce",
									  "authorizationCode": "apple_authorization_code"
									}
									""")
					)
			)
	)
	@SecurityRequirements()
	@ApiResponse(
			responseCode = "200",
			description = "애플 로그인에 성공했습니다.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "APPLE_LOGIN_SUCCESS",
							  "message": "애플 로그인에 성공했습니다.",
							  "data": {
							    "accessToken": "eyJhbGciOi...",
							    "refreshToken": "eyJhbGciOi...",
							    "tokenType": "Bearer",
							    "accessTokenExpiresIn": 3600,
							    "refreshTokenExpiresIn": 1209600,
							    "userId": 1,
							    "onboardingStatus": "STARTED"
							  }
							}
							""")
			)
	)
	@ApiExceptions({
			AppleClientIdNotConfiguredException.class,
			AppleIdTokenVerificationFailedException.class,
			AppleIdTokenAudienceMismatchException.class,
			AppleIdTokenNonceMismatchException.class,
			AppleIdTokenSubjectMissingException.class,
			AppleLoginFailedException.class
	})
	CommonResponse<AppleLoginResponse> appleLogin(AppleLoginRequest request);
}
