// 인증 HTTP API의 로그아웃 요청과 응답 형식을 검증하는 테스트
package com.sopt.nearby.user.adapter.in.web.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.application.LogoutUserCommand;
import com.sopt.nearby.user.application.LogoutUserResult;
import com.sopt.nearby.user.application.RefreshTokenCommand;
import com.sopt.nearby.user.application.RefreshTokenResult;
import com.sopt.nearby.user.exception.InvalidLogoutRequestException;
import com.sopt.nearby.user.exception.InvalidRefreshTokenException;
import com.sopt.nearby.user.exception.InvalidTokenRefreshRequestException;
import com.sopt.nearby.user.exception.RefreshTokenAlreadyRevokedException;
import com.sopt.nearby.user.port.in.LogoutUserUseCase;
import com.sopt.nearby.user.port.in.RefreshTokenUseCase;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AuthControllerTest {

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeLogoutUserUseCase logoutUserUseCase = new FakeLogoutUserUseCase();
	private final FakeRefreshTokenUseCase refreshTokenUseCase = new FakeRefreshTokenUseCase();
	private final Principal principal = () -> "7";

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders
				.standaloneSetup(new AuthController(logoutUserUseCase, refreshTokenUseCase))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		logoutUserUseCase.result = new LogoutUserResult(true);
		logoutUserUseCase.exception = null;
		logoutUserUseCase.command = null;
		refreshTokenUseCase.result = new RefreshTokenResult(
				"access-token", "refresh-token", "Bearer", 3600, 1209600
		);
		refreshTokenUseCase.exception = null;
		refreshTokenUseCase.command = null;
	}

	@Test
	void returnsLoggedOutWhenLogoutSucceeds() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.principal(principal)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("refresh-token"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("LOGOUT_USER")))
				.andExpect(jsonPath("$.message", is("로그아웃되었어요.")))
				.andExpect(jsonPath("$.data.loggedOut", is(true)));

		org.junit.jupiter.api.Assertions.assertEquals(7L, logoutUserUseCase.command.userId());
		org.junit.jupiter.api.Assertions.assertEquals("refresh-token", logoutUserUseCase.command.refreshToken());
	}

	@Test
	void returnsInvalidLogoutRequestWhenRefreshTokenIsMissing() throws Exception {
		mockMvc.perform(post("/api/auth/logout")
						.principal(principal)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("INVALID_LOGOUT_REQUEST")))
				.andExpect(jsonPath("$.message", is("올바르지 않은 로그아웃 요청입니다.")));
	}

	@Test
	void returnsUnauthorizedWhenRefreshRequestTokenIsInvalid() throws Exception {
		logoutUserUseCase.exception = new InvalidRefreshTokenException();

		mockMvc.perform(post("/api/auth/logout")
						.principal(principal)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("invalid-refresh-token"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status", is(401)))
				.andExpect(jsonPath("$.code", is("INVALID_REFRESH_TOKEN")))
				.andExpect(jsonPath("$.message", is("유효하지 않은 refreshToken입니다.")));
	}

	@Test
	void returnsConflictWhenRefreshTokenAlreadyRevoked() throws Exception {
		logoutUserUseCase.exception = new RefreshTokenAlreadyRevokedException();

		mockMvc.perform(post("/api/auth/logout")
						.principal(principal)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("revoked-refresh-token"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status", is(409)))
				.andExpect(jsonPath("$.code", is("REFRESH_TOKEN_ALREADY_REVOKED")))
				.andExpect(jsonPath("$.message", is("이미 만료 처리된 refreshToken입니다.")));
	}

	@Test
	void returnsRotatedTokensWhenRefreshSucceeds() throws Exception {
		mockMvc.perform(post("/api/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new Request("refresh-token"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("REFRESH_TOKEN")))
				.andExpect(jsonPath("$.message", is("토큰을 재발급했어요.")))
				.andExpect(jsonPath("$.data.accessToken", is("access-token")))
				.andExpect(jsonPath("$.data.refreshToken", is("refresh-token")))
				.andExpect(jsonPath("$.data.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.data.accessTokenExpiresIn", is(3600)))
				.andExpect(jsonPath("$.data.refreshTokenExpiresIn", is(1209600)));

		org.junit.jupiter.api.Assertions.assertEquals("refresh-token", refreshTokenUseCase.command.refreshToken());
	}

	@Test
	void returnsBadRequestWhenRefreshTokenIsMissing() throws Exception {
		refreshTokenUseCase.exception = new InvalidTokenRefreshRequestException();

		mockMvc.perform(post("/api/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("INVALID_TOKEN_REFRESH_REQUEST")))
				.andExpect(jsonPath("$.message", is("올바르지 않은 토큰 재발급 요청입니다.")));
	}

	@Test
	void returnsUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
		refreshTokenUseCase.exception = new InvalidRefreshTokenException();

		mockMvc.perform(post("/api/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new Request("invalid-refresh-token"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("INVALID_REFRESH_TOKEN")));
	}

	@Test
	void returnsConflictWhenRefreshTokenWasAlreadyRevoked() throws Exception {
		refreshTokenUseCase.exception = new RefreshTokenAlreadyRevokedException();

		mockMvc.perform(post("/api/auth/refresh")
					.contentType(MediaType.APPLICATION_JSON)
					.content(objectMapper.writeValueAsString(new Request("revoked-refresh-token"))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("REFRESH_TOKEN_ALREADY_REVOKED")));
	}

	private record Request(String refreshToken) {
	}

	private static final class FakeLogoutUserUseCase implements LogoutUserUseCase {

		private LogoutUserResult result;
		private RuntimeException exception;
		private LogoutUserCommand command;

		@Override
		public LogoutUserResult logout(final LogoutUserCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			if (command.refreshToken() == null || command.refreshToken().isBlank()) {
				throw new InvalidLogoutRequestException();
			}
			return result;
		}
	}

	private static final class FakeRefreshTokenUseCase implements RefreshTokenUseCase {

		private RefreshTokenResult result;
		private RuntimeException exception;
		private RefreshTokenCommand command;

		@Override
		public RefreshTokenResult refresh(final RefreshTokenCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			if (command.refreshToken() == null || command.refreshToken().isBlank()) {
				throw new InvalidTokenRefreshRequestException();
			}
			return result;
		}
	}
}
