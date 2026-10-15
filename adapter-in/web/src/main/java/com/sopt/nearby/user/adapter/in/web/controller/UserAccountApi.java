// 회원 계정 API의 Swagger 문서 계약을 정의하는 인터페이스
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import com.sopt.nearby.user.adapter.in.web.dto.response.WithdrawUserResponse;
import com.sopt.nearby.user.exception.AppleReauthenticationRequiredException;
import com.sopt.nearby.user.exception.SocialAccountUnlinkFailedException;
import com.sopt.nearby.user.exception.UserAccountNotFoundException;
import com.sopt.nearby.user.exception.UserAlreadyWithdrawnException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "User Account", description = "회원 계정 API")
public interface UserAccountApi {

	@Operation(
			summary = "회원 탈퇴",
			description = "로그인한 회원의 소셜 계정 연동을 해제하고 개인정보를 정리합니다.",
			security = @SecurityRequirement(name = "bearerAuth")
	)
	@ApiResponse(
			responseCode = "200",
			description = "회원 탈퇴가 완료되었어요.",
			content = @Content(
					mediaType = "application/json",
					examples = @ExampleObject(value = """
							{
							  "status": 200,
							  "code": "WITHDRAW_USER",
							  "message": "회원 탈퇴가 완료되었어요.",
							  "data": { "withdrawn": true }
							}
							""")
			)
	)
	@ApiExceptions({
			UserAccountNotFoundException.class,
			UserAlreadyWithdrawnException.class,
			AppleReauthenticationRequiredException.class,
			SocialAccountUnlinkFailedException.class
	})
	CommonResponse<WithdrawUserResponse> withdraw(
			@Parameter(hidden = true) Principal principal
	);
}
