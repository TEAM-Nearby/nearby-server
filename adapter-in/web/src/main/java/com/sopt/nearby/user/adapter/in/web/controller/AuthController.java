// 인증 HTTP 요청을 유스케이스로 전달하는 컨트롤러
package com.sopt.nearby.user.adapter.in.web.controller;

import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.user.adapter.in.web.dto.request.LogoutRequest;
import com.sopt.nearby.user.adapter.in.web.dto.request.RefreshTokenRequest;
import com.sopt.nearby.user.adapter.in.web.dto.response.LogoutResponse;
import com.sopt.nearby.user.adapter.in.web.dto.response.RefreshTokenResponse;
import com.sopt.nearby.user.adapter.in.web.response.AuthSuccessCode;
import com.sopt.nearby.user.application.LogoutUserCommand;
import com.sopt.nearby.user.application.LogoutUserResult;
import com.sopt.nearby.user.application.RefreshTokenCommand;
import com.sopt.nearby.user.port.in.LogoutUserUseCase;
import com.sopt.nearby.user.port.in.RefreshTokenUseCase;
import java.security.Principal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController implements AuthApi {

	private final LogoutUserUseCase logoutUserUseCase;
	private final RefreshTokenUseCase refreshTokenUseCase;

	public AuthController(
			final LogoutUserUseCase logoutUserUseCase,
			final RefreshTokenUseCase refreshTokenUseCase
	) {
		this.logoutUserUseCase = logoutUserUseCase;
		this.refreshTokenUseCase = refreshTokenUseCase;
	}

	@Override
	@PostMapping("/logout")
	public CommonResponse<LogoutResponse> logout(
			@RequestBody(required = false) final LogoutRequest request,
			final Principal principal
	) {
		Long userId = Long.valueOf(principal.getName());
		LogoutUserResult result = logoutUserUseCase.logout(toCommand(request, userId));
		return CommonResponse.success(
				AuthSuccessCode.LOGOUT_USER,
				LogoutResponse.from(result)
		);
	}

	@Override
	@PostMapping("/refresh")
	public CommonResponse<RefreshTokenResponse> refresh(
			@RequestBody(required = false) final RefreshTokenRequest request
	) {
		return CommonResponse.success(
				AuthSuccessCode.REFRESH_TOKEN,
				RefreshTokenResponse.from(refreshTokenUseCase.refresh(toCommand(request)))
		);
	}

	private LogoutUserCommand toCommand(final LogoutRequest request, final Long userId) {
		if (request == null) {
			return new LogoutUserCommand(userId, null);
		}
		return request.toCommand(userId);
	}

	private RefreshTokenCommand toCommand(
			final RefreshTokenRequest request
	) {
		if (request == null) {
			return new RefreshTokenCommand(null);
		}
		return request.toCommand();
	}
}
