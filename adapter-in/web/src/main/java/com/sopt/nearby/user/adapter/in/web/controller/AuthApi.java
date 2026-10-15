// 인증 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.request.LogoutRequest;
import com.sopt.nearby.user.adapter.in.web.dto.request.RefreshTokenRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.LogoutResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.RefreshTokenResponse;
import com.sopt.nearby.user.exception.InvalidLogoutRequestException;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.InvalidTokenRefreshRequestException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "Auth", description = "인증 API")
public interface AuthApi {

	@Operation(
			summary = "로그아웃",
			description = "JWT 액세스 토큰으로 인증된 사용자의 리프레시 토큰을 만료 처리합니다.",
			security = @SecurityRequirement(name = "bearerAuth"),
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = LogoutRequest.class),
							examples = @ExampleObject(value = """
									{
									  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
									}
									""")
					)
			)
	)
	@ApiResponse(
			responseCode = "200",
			description = "로그아웃되었어요.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "LOGOUT_USER",
							  "message": "로그아웃되었어요.",
							  "data": {
							    "loggedOut": true
							  }
							}
							""")
			)
	)
	@ApiExceptions({
			InvalidLogoutRequestException.class,
			InvalidRefreshTokenException.class,
			RefreshTokenAlreadyRevokedException.class
	})
	CommonResponse<LogoutResponse> logout(
			LogoutRequest request,
			@Parameter(hidden = true)
			Principal principal
	);

	@Operation(
			summary = "토큰 재발급",
			description = "유효한 리프레시 토큰으로 새 액세스 토큰과 리프레시 토큰을 발급하고 기존 리프레시 토큰을 폐기합니다.",
			requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
					required = true,
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = RefreshTokenRequest.class),
							examples = @ExampleObject(value = """
									{
									  "refreshToken": "refresh_token_value"
									}
									""")
					)
			)
	)
	@SecurityRequirements()
	@ApiResponse(
			responseCode = "200",
			description = "토큰을 재발급했어요.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "REFRESH_TOKEN",
							  "message": "토큰을 재발급했어요.",
							  "data": {
							    "accessToken": "eyJhbGciOi...",
							    "refreshToken": "refresh_token_value",
							    "tokenType": "Bearer",
							    "accessTokenExpiresIn": 3600,
							    "refreshTokenExpiresIn": 1209600
							  }
							}
							""")
			)
	)
	@ApiExceptions({
			InvalidTokenRefreshRequestException.class,
			InvalidRefreshTokenException.class,
			RefreshTokenAlreadyRevokedException.class
	})
	CommonResponse<RefreshTokenResponse> refresh(RefreshTokenRequest request);
}
