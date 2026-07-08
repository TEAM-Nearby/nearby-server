// 내가 작성한 동행 모집글 컨트롤러의 경로와 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.ReadMyCompanionPostsResult;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.port.in.ReadMyCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MyCompanionPostControllerTest {

	private MockMvc mockMvc;
	private FakeReadMyCompanionPostsUseCase readUseCase;

	@BeforeEach
	void setUp() {
		readUseCase = new FakeReadMyCompanionPostsUseCase();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new MyCompanionPostController(readUseCase))
				.setMessageConverters(jsonMessageConverter())
				.setControllerAdvice(new GlobalExceptionHandler())
				.build();
	}

	@Test
	void getsMyCompanionPosts() throws Exception {
		readUseCase.result = new ReadMyCompanionPostsResult(List.of(new ReadMyCompanionPostsResult.Post(
				1L,
				"바르셀로나",
				LocalDateTime.of(2026, 6, 29, 19, 0),
				new ReadMyCompanionPostsResult.Place(
						"google-place-id",
						"시우다드 콘달",
						new BigDecimal("41.39020500"),
						new BigDecimal("2.16354800")
				),
				3,
				4,
				"같이 밥 먹어요.",
				List.of(ReviewKeyword.PUNCTUAL, ReviewKeyword.GOOD_MANNERS)
		)));

		mockMvc.perform(get("/api/v1/users/me/recruitment-posts")
						.principal(principal("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(200))
				.andExpect(jsonPath("$.code").value("READ_MY_COMPANION_POSTS"))
				.andExpect(jsonPath("$.message").value("내가 작성한 동행 모집글 목록을 조회했어요."))
				.andExpect(jsonPath("$.data.posts[0].postId").value(1))
				.andExpect(jsonPath("$.data.posts[0].cityName").value("바르셀로나"))
				.andExpect(jsonPath("$.data.posts[0].scheduledAt").value("2026-06-29T19:00:00"))
				.andExpect(jsonPath("$.data.posts[0].place.googlePlaceId").value("google-place-id"))
				.andExpect(jsonPath("$.data.posts[0].place.name").value("시우다드 콘달"))
				.andExpect(jsonPath("$.data.posts[0].place.latitude").value(41.39020500))
				.andExpect(jsonPath("$.data.posts[0].place.longitude").value(2.16354800))
				.andExpect(jsonPath("$.data.posts[0].currentParticipants").value(3))
				.andExpect(jsonPath("$.data.posts[0].maxParticipants").value(4))
				.andExpect(jsonPath("$.data.posts[0].content").value("같이 밥 먹어요."))
				.andExpect(jsonPath("$.data.posts[0].reviewKeywords[0]").value("PUNCTUAL"))
				.andExpect(jsonPath("$.data.posts[0].reviewKeywords[1]").value("GOOD_MANNERS"));

		assertEquals(7L, readUseCase.userId);
	}

	@Test
	void returnsEmptyPosts() throws Exception {
		readUseCase.result = new ReadMyCompanionPostsResult(List.of());

		mockMvc.perform(get("/api/v1/users/me/recruitment-posts")
						.principal(principal("7")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.posts").isArray())
				.andExpect(jsonPath("$.data.posts").isEmpty());
	}

	private Principal principal(final String name) {
		return () -> name;
	}

	private MappingJackson2HttpMessageConverter jsonMessageConverter() {
		ObjectMapper objectMapper = new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		return new MappingJackson2HttpMessageConverter(objectMapper);
	}

	private static final class FakeReadMyCompanionPostsUseCase implements ReadMyCompanionPostsUseCase {

		private ReadMyCompanionPostsResult result;
		private Long userId;

		@Override
		public ReadMyCompanionPostsResult getPosts(final Long userId) {
			this.userId = userId;
			return result;
		}
	}
}
