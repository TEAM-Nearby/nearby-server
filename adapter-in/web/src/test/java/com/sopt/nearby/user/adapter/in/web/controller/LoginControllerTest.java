// 카카오 로그인 HTTP API의 요청 검증과 응답 형식을 검증하는 테스트
package com.sopt.nearby.user.adapter.in.web.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.application.AppleLoginCommand;
import com.sopt.nearby.user.application.AppleLoginResult;
import com.sopt.nearby.user.application.KakaoLoginCommand;
import com.sopt.nearby.user.application.KakaoLoginResult;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.AppleLoginFailedException;
import com.sopt.nearby.user.exception.KakaoLoginFailedException;
import com.sopt.nearby.user.port.in.AppleLoginUseCase;
import com.sopt.nearby.user.port.in.KakaoLoginUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class LoginControllerTest {

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeKakaoLoginUseCase kakaoLoginUseCase = new FakeKakaoLoginUseCase();
	private final FakeAppleLoginUseCase appleLoginUseCase = new FakeAppleLoginUseCase();

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new LoginController(kakaoLoginUseCase, appleLoginUseCase))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.build();
		kakaoLoginUseCase.result = new KakaoLoginResult(
				"access-token",
				"refresh-token",
				"Bearer",
				3600,
				1209600,
				1L,
				UserOnboardingStatus.COMPANION_PROFILE_COMPLETED
		);
		kakaoLoginUseCase.exception = null;
		kakaoLoginUseCase.command = null;
		appleLoginUseCase.result = new AppleLoginResult(
				"apple-access-token",
				"apple-refresh-token",
				"Bearer",
				3600,
				1209600,
				2L,
				UserOnboardingStatus.STARTED
		);
		appleLoginUseCase.command = null;
		appleLoginUseCase.exception = null;
	}

	@Test
	void returnsTokensWhenKakaoLoginSucceeds() throws Exception {
		mockMvc.perform(post("/api/login/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("id-token", "nonce"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("KAKAO_LOGIN_SUCCESS")))
				.andExpect(jsonPath("$.message", is("카카오 로그인에 성공했습니다.")))
				.andExpect(jsonPath("$.data.accessToken", is("access-token")))
				.andExpect(jsonPath("$.data.refreshToken", is("refresh-token")))
				.andExpect(jsonPath("$.data.tokenType", is("Bearer")))
				.andExpect(jsonPath("$.data.accessTokenExpiresIn", is(3600)))
				.andExpect(jsonPath("$.data.refreshTokenExpiresIn", is(1209600)))
				.andExpect(jsonPath("$.data.userId", is(1)))
				.andExpect(jsonPath("$.data.onboardingStatus", is("COMPLETED")));
	}

	@Test
	void returnsValidationErrorWhenRequiredValueIsMissing() throws Exception {
		mockMvc.perform(post("/api/login/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("", "nonce"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.message", is("필수 요청값이 누락되었습니다.")));
	}

	@Test
	void returnsUnauthorizedWhenKakaoLoginFails() throws Exception {
		kakaoLoginUseCase.exception = new KakaoLoginFailedException();

		mockMvc.perform(post("/api/login/kakao")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("bad-token", "nonce"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status", is(401)))
				.andExpect(jsonPath("$.code", is("KAKAO_LOGIN_FAILED")))
				.andExpect(jsonPath("$.message", is("카카오 ID 토큰이 유효하지 않거나 OIDC 검증에 실패했습니다.")));
	}

	@Test
	void returnsTokensWhenAppleLoginSucceeds() throws Exception {
		mockMvc.perform(post("/api/login/apple")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("apple-id-token", "nonce"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("APPLE_LOGIN_SUCCESS")))
				.andExpect(jsonPath("$.message", is("애플 로그인에 성공했습니다.")))
				.andExpect(jsonPath("$.data.accessToken", is("apple-access-token")))
				.andExpect(jsonPath("$.data.userId", is(2)))
				.andExpect(jsonPath("$.data.onboardingStatus", is("STARTED")));
	}

	@Test
	void returnsUnauthorizedWhenAppleLoginFails() throws Exception {
		appleLoginUseCase.exception = new AppleLoginFailedException();

		mockMvc.perform(post("/api/login/apple")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("bad-token", "nonce"))))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", is("APPLE_LOGIN_FAILED")));
	}

	private record Request(String idToken, String nonce) {
	}

	static class FakeKakaoLoginUseCase implements KakaoLoginUseCase {

		private KakaoLoginResult result;
		private RuntimeException exception;
		private KakaoLoginCommand command;

		@Override
		public KakaoLoginResult login(final KakaoLoginCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}

	static class FakeAppleLoginUseCase implements AppleLoginUseCase {

		private AppleLoginResult result;
		private AppleLoginCommand command;
		private RuntimeException exception;

		@Override
		public AppleLoginResult login(final AppleLoginCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}
}
