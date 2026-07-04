// 동행 매칭 미리보기 컨트롤러의 인증 사용자 전달과 응답 형식을 검증하는 테스트
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionMatchControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionMatchPreviewUseCase useCase;
    private FakeReadCompanionMatchesUseCase readCompanionMatchesUseCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionMatchPreviewUseCase();
        readCompanionMatchesUseCase = new FakeReadCompanionMatchesUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionMatchController(useCase, readCompanionMatchesUseCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passesAuthenticatedUserIdFromPrincipalToUseCase() throws Exception {
        useCase.result = new CompanionMatchPreview(
                10L,
                List.of(
                        new CompanionMatchPreview.Member(7L, "https://image.example/a.png", "여행자A"),
                        new CompanionMatchPreview.Member(8L, null, "여행자B")
                ),
                new CompanionMatchPreview.Post(20L, "함께 밥 먹을 동행을 구해요.")
        );

        mockMvc.perform(get("/api/companion-matches/{matchId}/preview", 10L)
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_MATCH_PREVIEW"))
                .andExpect(jsonPath("$.message").value("매칭된 동행 미리보기 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.matchId").value(10))
                .andExpect(jsonPath("$.data.members[0].memberId").value(7))
                .andExpect(jsonPath("$.data.members[0].profileImageUrl").value("https://image.example/a.png"))
                .andExpect(jsonPath("$.data.members[0].nickname").value("여행자A"))
                .andExpect(jsonPath("$.data.members[1].memberId").value(8))
                .andExpect(jsonPath("$.data.members[1].profileImageUrl").value(nullValue()))
                .andExpect(jsonPath("$.data.members[1].nickname").value("여행자B"))
                .andExpect(jsonPath("$.data.companionPost.postId").value(20))
                .andExpect(jsonPath("$.data.companionPost.content").value("함께 밥 먹을 동행을 구해요."));

        assertEquals(10L, useCase.matchId);
        assertEquals(7L, useCase.userId);
    }

    @Test
    void passesAuthenticatedUserIdFromPrincipalToReadMatchesUseCase() throws Exception {
        readCompanionMatchesUseCase.result = List.of(new CompanionMatchSummary(
                1L,
                "호스트A",
                "시우다드콘달",
                LocalDateTime.of(2026, 6, 29, 18, 30),
                "오늘 저녁 바르셀로나에서 같이 타파스 드실 분 구해요",
                CompanionMatchStatus.MATCHED
        ));

        mockMvc.perform(get("/api/companion-matches")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_COMPANION_MATCHES"))
                .andExpect(jsonPath("$.message").value("매칭된 동행 목록을 조회했어요."))
                .andExpect(jsonPath("$.data.matches[0].matchId").value(1))
                .andExpect(jsonPath("$.data.matches[0].hostNickname").value("호스트A"))
                .andExpect(jsonPath("$.data.matches[0].placeName").value("시우다드콘달"))
                .andExpect(jsonPath("$.data.matches[0].meetingAt").value("2026-06-29T18:30:00"))
                .andExpect(jsonPath("$.data.matches[0].content")
                        .value("오늘 저녁 바르셀로나에서 같이 타파스 드실 분 구해요"))
                .andExpect(jsonPath("$.data.matches[0].matchStatus").value("MATCHED"));

        assertEquals(7L, readCompanionMatchesUseCase.userId);
    }

    @Test
    void returnsForbiddenWhenRequesterIsNotMatchParticipant() throws Exception {
        useCase.exception = new ForbiddenCompanionMatchException();

        mockMvc.perform(get("/api/companion-matches/{matchId}/preview", 10L)
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN_COMPANION_MATCH"))
                .andExpect(jsonPath("$.message").value("해당 매칭 정보를 조회할 권한이 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
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

    private static final class FakeReadCompanionMatchPreviewUseCase implements ReadCompanionMatchPreviewUseCase {

        private CompanionMatchPreview result;
        private RuntimeException exception;
        private Long matchId;
        private Long userId;

        @Override
        public CompanionMatchPreview getPreview(final Long matchId, final Long userId) {
            this.matchId = matchId;
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeReadCompanionMatchesUseCase implements ReadCompanionMatchesUseCase {

        private List<CompanionMatchSummary> result = List.of();
        private Long userId;

        @Override
        public List<CompanionMatchSummary> getMatches(final Long userId) {
            this.userId = userId;
            return result;
        }
    }
}
