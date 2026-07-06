// Spring Security 인증 정책의 공개 경로와 보호 경로를 검증하는 테스트
package com.sopt.nearby;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "management.health.redis.enabled=false")
@AutoConfigureMockMvc
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void permitsKakaoLoginWithoutBearerToken() throws Exception {
		mockMvc.perform(post("/api/kakao/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void permitsHealthWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void permitsOpenApiDocsWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk());
	}

	@Test
	void openApiDocsUsesForwardedHttpsScheme() throws Exception {
		mockMvc.perform(get("/v3/api-docs")
						.header("X-Forwarded-Proto", "https")
						.header("X-Forwarded-Host", "api.nearby.test"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.servers[0].url").value("https://api.nearby.test"));
	}

	@Test
	void documentsCompanionPostsOnboardingRequiredResponse() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.paths['/api/companion-posts'].get.responses['403']"
						+ ".content['application/json'].examples.ONBOARDING_REQUIRED.value.code")
						.value("ONBOARDING_REQUIRED"));
	}

	@Test
	void rejectsOtherApiWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/protected"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void rejectsOnboardingPhoneVerificationWithoutBearerTokenAsCommonJson() throws Exception {
		mockMvc.perform(post("/api/onboarding/phone-verifications")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"phoneNumber":"01012345678"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다."))
				.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	void rejectsOnboardingPhoneVerificationConfirmWithoutBearerTokenAsCommonJson() throws Exception {
		mockMvc.perform(patch("/api/onboarding/phone-verifications/{phoneVerificationId}", 10L)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"verificationCode":"123456"}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
				.andExpect(jsonPath("$.message").value("인증이 필요합니다."))
				.andExpect(jsonPath("$.data").value(nullValue()));
	}
}
