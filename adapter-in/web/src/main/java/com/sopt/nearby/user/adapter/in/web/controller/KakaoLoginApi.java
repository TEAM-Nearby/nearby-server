// 카카오 로그인 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.request.KakaoLoginRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.KakaoLoginResponse;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Kakao Login", description = "카카오 소셜 로그인 API")
public interface KakaoLoginApi {

	@Operation(
			summary = "카카오 로그인",
			description = "카카오 iOS SDK에서 받은 ID 토큰과 nonce로 Nearby 토큰을 발급합니다.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = KakaoLoginRequest.class),
							examples = @ExampleObject(value = """
									{
									  "idToken": "kakao_oidc_id_token",
									  "nonce": "login_request_nonce"
									}
									""")
					)
			)
	)
	@SecurityRequirements()
	@ApiResponse(
			responseCode = "200",
			description = "카카오 로그인에 성공했습니다.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "KAKAO_LOGIN_SUCCESS",
							  "message": "카카오 로그인에 성공했습니다.",
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
	@ApiExceptions(KakaoLoginFailedException.class)
	CommonResponse<KakaoLoginResponse> login(KakaoLoginRequest request);
}
