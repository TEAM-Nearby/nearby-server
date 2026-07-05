// 동행 프로필 온보딩 HTTP API를 검증하는 테스트
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sopt.nearby.companion.application.IssueProfileImageUploadUrlCommand;
import com.sopt.nearby.companion.application.ProfileImageUploadUrlResult;
import com.sopt.nearby.companion.application.RegisterCompanionProfileCommand;
import com.sopt.nearby.companion.application.RegisteredCompanionProfileResult;
import com.sopt.nearby.companion.domain.exception.DuplicateNicknameException;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.IssueProfileImageUploadUrlUseCase;
import com.sopt.nearby.companion.port.in.RegisterCompanionProfileUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.exception.PhoneVerificationRequiredException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

class OnboardingCompanionProfileControllerTest {

	private MockMvc mockMvc;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final FakeIssueProfileImageUploadUrlUseCase issueUseCase =
			new FakeIssueProfileImageUploadUrlUseCase();
	private final FakeRegisterCompanionProfileUseCase registerUseCase = new FakeRegisterCompanionProfileUseCase();

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new OnboardingCompanionProfileController(issueUseCase, registerUseCase))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setValidator(validator)
				.setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
				.build();
		SecurityContextHolder.clearContext();
		authenticate("1");
		issueUseCase.command = null;
		issueUseCase.result = new ProfileImageUploadUrlResult(
				"https://s3.ap-northeast-2.amazonaws.com/nearby/profiles/1/profile.jpg",
				"https://cdn.nearby.com/profiles/1/profile.jpg",
				"PUT",
				300,
				Map.of("Content-Type", "image/jpeg")
		);
		registerUseCase.command = null;
		registerUseCase.exception = null;
		registerUseCase.result = new RegisteredCompanionProfileResult(
				5L,
				"여행친구",
				UserGender.FEMALE,
				"혼자 여행도 같이 여행도 좋아해요",
				"https://cdn.nearby.com/profiles/1/profile.jpg",
				List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE),
				"COMPLETED"
		);
	}

	@Test
	void issuesProfileImageUploadUrl() throws Exception {
		mockMvc.perform(post("/api/onboarding/profile-images/presigned-url")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new UploadUrlRequest(
								"profile.jpg",
								"image/jpeg",
								524_288L
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("PROFILE_IMAGE_UPLOAD_URL_ISSUED")))
				.andExpect(jsonPath("$.message", is("Presigned URL 발급에 성공했습니다.")))
				.andExpect(jsonPath("$.data.uploadUrl", is(
						"https://s3.ap-northeast-2.amazonaws.com/nearby/profiles/1/profile.jpg"
				)))
				.andExpect(jsonPath("$.data.imageUrl", is("https://cdn.nearby.com/profiles/1/profile.jpg")))
				.andExpect(jsonPath("$.data.method", is("PUT")))
				.andExpect(jsonPath("$.data.expiresIn", is(300)))
				.andExpect(jsonPath("$.data.headers.Content-Type", is("image/jpeg")));

		assertNotNull(issueUseCase.command);
		assertEquals(1L, issueUseCase.command.userId());
		assertEquals("profile.jpg", issueUseCase.command.fileName());
		assertEquals("image/jpeg", issueUseCase.command.contentType());
		assertEquals(524_288L, issueUseCase.command.fileSize());
	}

	@Test
	void createsCompanionProfile() throws Exception {
		mockMvc.perform(post("/api/onboarding/companion-profiles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(
								"여행친구",
								"FEMALE",
								"혼자 여행도 같이 여행도 좋아해요",
								"https://cdn.nearby.com/profiles/1/profile.jpg",
								List.of("PLANNED", "FOODIE")
						))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", is(200)))
				.andExpect(jsonPath("$.code", is("COMPANION_PROFILE_CREATED")))
				.andExpect(jsonPath("$.message", is("동행 프로필 등록이 완료되었습니다.")))
				.andExpect(jsonPath("$.data.profileId", is(5)))
				.andExpect(jsonPath("$.data.nickname", is("여행친구")))
				.andExpect(jsonPath("$.data.gender", is("FEMALE")))
				.andExpect(jsonPath("$.data.intro", is("혼자 여행도 같이 여행도 좋아해요")))
				.andExpect(jsonPath("$.data.profileImageUrl", is("https://cdn.nearby.com/profiles/1/profile.jpg")))
				.andExpect(jsonPath("$.data.travelStyleKeywords[0]", is("PLANNED")))
				.andExpect(jsonPath("$.data.travelStyleKeywords[1]", is("FOODIE")))
				.andExpect(jsonPath("$.data.onboardingStatus", is("COMPLETED")));

		assertNotNull(registerUseCase.command);
		assertEquals(1L, registerUseCase.command.userId());
		assertEquals("여행친구", registerUseCase.command.nickname());
		assertEquals(UserGender.FEMALE, registerUseCase.command.gender());
		assertEquals("혼자 여행도 같이 여행도 좋아해요", registerUseCase.command.intro());
		assertEquals("https://cdn.nearby.com/profiles/1/profile.jpg", registerUseCase.command.profileImageUrl());
		assertIterableEquals(
				List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE),
				registerUseCase.command.travelStyleKeywords()
		);
	}

	@Test
	void rejectsDuplicateTravelStyleKeywords() throws Exception {
		mockMvc.perform(post("/api/onboarding/companion-profiles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(
								"여행친구",
								"FEMALE",
								null,
								null,
								List.of("FOODIE", "FOODIE")
						))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.message", is("필수값 누락되었거나 형식에 오류가 발생했습니다.")));
	}

	@Test
	void rejectsUnsupportedImageContentType() throws Exception {
		mockMvc.perform(post("/api/onboarding/profile-images/presigned-url")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new UploadUrlRequest(
								"profile.gif",
								"image/gif",
								524_288L
						))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", is(400)))
				.andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.message", is("지원하지 않는 이미지 형식이거나 또는 파일 크기를 초과했습니다.")));
	}

	@Test
	void returnsConflictWhenPhoneVerificationIsRequired() throws Exception {
		registerUseCase.exception = new PhoneVerificationRequiredException();

		mockMvc.perform(post("/api/onboarding/companion-profiles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(
								"여행친구",
								"FEMALE",
								null,
								null,
								List.of("FOODIE")
						))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status", is(409)))
				.andExpect(jsonPath("$.code", is("PHONE_VERIFICATION_REQUIRED")))
				.andExpect(jsonPath("$.message", is("휴대폰 인증이 완료되지 않았습니다.")));
	}

	@Test
	void returnsConflictWhenNicknameIsDuplicated() throws Exception {
		registerUseCase.exception = new DuplicateNicknameException();

		mockMvc.perform(post("/api/onboarding/companion-profiles")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new RegisterRequest(
								"여행친구",
								"FEMALE",
								null,
								null,
								List.of("FOODIE")
						))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.status", is(409)))
				.andExpect(jsonPath("$.code", is("DUPLICATE_NICKNAME")))
				.andExpect(jsonPath("$.message", is("이미 사용 중인 닉네임입니다.")));
	}

	private static void authenticate(final String subject) {
		Jwt jwt = Jwt.withTokenValue("access-token")
				.header("alg", "none")
				.subject(subject)
				.build();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken(jwt, null));
	}

	private record UploadUrlRequest(String fileName, String contentType, long fileSize) {
	}

	private record RegisterRequest(
			String nickname,
			String gender,
			String intro,
			String profileImageUrl,
			List<String> travelStyleKeywords
	) {
	}

	static class FakeIssueProfileImageUploadUrlUseCase implements IssueProfileImageUploadUrlUseCase {

		private IssueProfileImageUploadUrlCommand command;
		private ProfileImageUploadUrlResult result;

		@Override
		public ProfileImageUploadUrlResult issue(final IssueProfileImageUploadUrlCommand command) {
			this.command = command;
			return result;
		}
	}

	static class FakeRegisterCompanionProfileUseCase implements RegisterCompanionProfileUseCase {

		private RegisterCompanionProfileCommand command;
		private RegisteredCompanionProfileResult result;
		private RuntimeException exception;

		@Override
		public RegisteredCompanionProfileResult register(final RegisterCompanionProfileCommand command) {
			this.command = command;
			if (exception != null) {
				throw exception;
			}
			return result;
		}
	}
}
