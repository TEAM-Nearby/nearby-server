// 온보딩 휴대폰 인증 문자 발송 HTTP API를 검증하는 테스트
package com.sopt.nearby.user.adapter.in.web.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.adapter.in.web.dto.request.SendPhoneVerificationCodeRequest;
import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeCommand;
import com.sopt.nearby.user.application.ConfirmPhoneVerificationCodeResult;
import com.sopt.nearby.user.application.SendPhoneVerificationCodeCommand;
import com.sopt.nearby.user.application.SendPhoneVerificationCodeResult;
import com.sopt.nearby.user.domain.model.UserOnboardingStatus;
import com.sopt.nearby.user.exception.PhoneVerificationCodeMismatchException;
import com.sopt.nearby.user.exception.PhoneVerificationExpiredException;
import com.sopt.nearby.user.exception.PhoneVerificationNotFoundException;
import com.sopt.nearby.user.exception.PhoneVerificationSendLimitExceededException;
import com.sopt.nearby.user.port.in.ConfirmPhoneVerificationCodeUseCase;
import com.sopt.nearby.user.port.in.SendPhoneVerificationCodeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OnboardingPhoneVerificationControllerTest {

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeSendPhoneVerificationCodeUseCase sendUseCase = new FakeSendPhoneVerificationCodeUseCase();
	private final FakeConfirmPhoneVerificationCodeUseCase confirmUseCase = new FakeConfirmPhoneVerificationCodeUseCase();

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new OnboardingPhoneVerificationController(sendUseCase, confirmUseCase))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
		sendUseCase.result = new SendPhoneVerificationCodeResult(10L, 180);
		sendUseCase.exception = null;
		sendUseCase.command = null;
		confirmUseCase.result = new ConfirmPhoneVerificationCodeResult(true, UserOnboardingStatus.PHONE_VERIFIED);
		confirmUseCase.exception = null;
		confirmUseCase.command = null;
		authenticate("1");
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void sendsPhoneVerificationCode() throws Exception {
		mockMvc.perform(post("/api/onboarding/phone-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("01012345678"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_CODE_SENT")))
				.andExpect(jsonPath("$.message", is("인증 문자를 발송에 성공했습니다.")))
				.andExpect(jsonPath("$.data.phoneVerificationId", is(10)))
				.andExpect(jsonPath("$.data.expiresIn", is(180)));

		assertNotNull(sendUseCase.command);
		assertEquals(1L, sendUseCase.command.userId());
		assertEquals("01012345678", sendUseCase.command.phoneNumber());
	}

	@Test
	void confirmsPhoneVerificationCode() throws Exception {
		mockMvc.perform(patch("/api/onboarding/phone-verifications/{phoneVerificationId}", 10L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ConfirmRequest("123456"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_CODE_CONFIRMED")))
				.andExpect(jsonPath("$.message", is("휴대폰 인증에 성공했습니다.")))
				.andExpect(jsonPath("$.data.phoneVerified", is(true)))
				.andExpect(jsonPath("$.data.onboardingStatus", is("PHONE_VERIFIED")));

		assertNotNull(confirmUseCase.command);
		assertEquals(1L, confirmUseCase.command.userId());
		assertEquals(10L, confirmUseCase.command.phoneVerificationId());
		assertEquals("123456", confirmUseCase.command.verificationCode());
	}

	@Test
	void returnsValidationErrorWhenPhoneNumberIsBlank() throws Exception {
		assertInvalidPhoneNumber("");
	}

	@Test
	void returnsValidationErrorWhenPhoneNumberHasTenDigits() throws Exception {
		assertInvalidPhoneNumber("0101234567");
	}

	@Test
	void returnsValidationErrorWhenPhoneNumberContainsHyphen() throws Exception {
		assertInvalidPhoneNumber("010-1234-5678");
	}

	@Test
	void returnsInternalServerErrorWhenSendLimitIsExceeded() throws Exception {
		sendUseCase.exception = new PhoneVerificationSendLimitExceededException();

		mockMvc.perform(post("/api/onboarding/phone-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request("01012345678"))))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status", is(500)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_SEND_LIMIT_EXCEEDED")))
				.andExpect(jsonPath("$.message", is("인증 문자 발송 횟수를 초과했습니다.")));
	}

	@Test
	void returnsBadRequestWhenVerificationCodeMismatches() throws Exception {
		confirmUseCase.exception = new PhoneVerificationCodeMismatchException();

		mockMvc.perform(patch("/api/onboarding/phone-verifications/{phoneVerificationId}", 10L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ConfirmRequest("000000"))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_CODE_MISMATCH")))
				.andExpect(jsonPath("$.message", is("인증 번호가 일치하지 않습니다.")));
	}

	@Test
	void returnsNotFoundWhenVerificationDoesNotExist() throws Exception {
		confirmUseCase.exception = new PhoneVerificationNotFoundException();

		mockMvc.perform(patch("/api/onboarding/phone-verifications/{phoneVerificationId}", 99L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ConfirmRequest("123456"))))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status", is(404)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_NOT_FOUND")))
				.andExpect(jsonPath("$.message", is("인증 요청이 발생하지 않았습니다.")));
	}

	@Test
	void returnsGoneWhenVerificationExpired() throws Exception {
		confirmUseCase.exception = new PhoneVerificationExpiredException();

		mockMvc.perform(patch("/api/onboarding/phone-verifications/{phoneVerificationId}", 10L)
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new ConfirmRequest("123456"))))
				.andExpect(status().isGone())
				.andExpect(jsonPath("$.status", is(410)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_EXPIRED")))
				.andExpect(jsonPath("$.message", is("인증 시간이 만료되었습니다.")));
	}

	@Test
	void documentsBearerAuthForSwaggerTryItOut() throws Exception {
		Method method = OnboardingPhoneVerificationApi.class.getMethod(
				"send",
				SendPhoneVerificationCodeRequest.class,
				Jwt.class
		);
		Operation operation = method.getAnnotation(Operation.class);

		assertNotNull(operation);
		assertEquals(1, operation.security().length);
		assertEquals("bearerAuth", operation.security()[0].name());
	}

	private void assertInvalidPhoneNumber(final String phoneNumber) throws Exception {
		mockMvc.perform(post("/api/onboarding/phone-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new Request(phoneNumber))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.message", is("전화번호 형식이 올바르지 않습니다.")));
	}

	private static void authenticate(final String subject) {
		Jwt jwt = Jwt.withTokenValue("access-token")
				.header("alg", "none")
				.subject(subject)
				.build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));
	}

	private record Request(String phoneNumber) {
	}

	private record ConfirmRequest(String verificationCode) {
	}

	static class FakeSendPhoneVerificationCodeUseCase implements SendPhoneVerificationCodeUseCase {

		private SendPhoneVerificationCodeResult result;
		private RuntimeException exception;
		private SendPhoneVerificationCodeCommand command;

		@Override
		public SendPhoneVerificationCodeResult send(final SendPhoneVerificationCodeCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}

	static class FakeConfirmPhoneVerificationCodeUseCase implements ConfirmPhoneVerificationCodeUseCase {

		private ConfirmPhoneVerificationCodeResult result;
		private RuntimeException exception;
		private ConfirmPhoneVerificationCodeCommand command;

		@Override
		public ConfirmPhoneVerificationCodeResult confirm(final ConfirmPhoneVerificationCodeCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}
}
