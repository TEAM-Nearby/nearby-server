// 회원 탈퇴 HTTP API의 인증 사용자 전달과 응답 형식을 검증하는 테스트
package com.sopt.nearby.user.adapter.in.web.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.application.WithdrawUserCommand;
import com.sopt.nearby.user.application.WithdrawUserResult;
import com.sopt.nearby.user.exception.AppleReauthenticationRequiredException;
import com.sopt.nearby.user.port.in.WithdrawUserUseCase;
import java.security.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class UserAccountControllerTest {

	private final FakeWithdrawUserUseCase useCase = new FakeWithdrawUserUseCase();
	private final Principal principal = () -> "7";
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(new UserAccountController(useCase))
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
		useCase.command = null;
		useCase.exception = null;
	}

	@Test
	void withdrawsAuthenticatedUser() throws Exception {
		mockMvc.perform(delete("/api/users/me").principal(principal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.code", is("WITHDRAW_USER")))
				.andExpect(jsonPath("$.message", is("회원 탈퇴가 완료되었어요.")))
				.andExpect(jsonPath("$.data.withdrawn", is(true)));

		assertEquals(7L, useCase.command.userId());
	}

	@Test
	void returnsConflictWhenAppleLoginIsRequiredAgain() throws Exception {
		useCase.exception = new AppleReauthenticationRequiredException();

		mockMvc.perform(delete("/api/users/me").principal(principal))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", is("APPLE_REAUTHENTICATION_REQUIRED")));
	}

	private static final class FakeWithdrawUserUseCase implements WithdrawUserUseCase {
		private WithdrawUserCommand command;
		private RuntimeException exception;

		@Override
		public WithdrawUserResult withdraw(final WithdrawUserCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return new WithdrawUserResult(true);
		}
	}
}
