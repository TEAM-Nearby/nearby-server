// Spring Security 인증 정책의 공개 경로와 보호 경로를 검증하는 테스트
package com.sopt.nearby;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
	void rejectsOtherApiWithoutBearerToken() throws Exception {
		mockMvc.perform(get("/api/protected"))
				.andExpect(status().isUnauthorized());
	}
}
