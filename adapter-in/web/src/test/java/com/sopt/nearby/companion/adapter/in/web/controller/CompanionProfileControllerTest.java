// 동행 프로필 상세 조회 컨트롤러의 요청 전달과 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.ReadCompanionProfileCommand;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileDetail;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.ReadCompanionProfileUseCase;
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

class CompanionProfileControllerTest {

    private MockMvc mockMvc;
    private FakeReadCompanionProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadCompanionProfileUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionProfileController(useCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsCompanionProfile() throws Exception {
        useCase.result = result(List.of(TravelStyleKeyword.PLANNED, TravelStyleKeyword.FOODIE));

        mockMvc.perform(get("/api/companion-profiles/5")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 프로필 조회에 성공했어요."))
                .andExpect(jsonPath("$.data.profileId").value(5))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.nickname").value("니어바이"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.birthYear").value(nullValue()))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.nearby.com/profiles/1.jpg"))
                .andExpect(jsonPath("$.data.intro").value("혼자 여행도 같이 여행도 좋아해요"))
                .andExpect(jsonPath("$.data.mannerScore").value(4.0))
                .andExpect(jsonPath("$.data.mannerKeywords[0]").value("FAST_RESPONSE"))
                .andExpect(jsonPath("$.data.mannerKeywords[1]").value("PUNCTUAL"))
                .andExpect(jsonPath("$.data.reviewCount").value(12))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.phoneVerifiedAt").value("2026-07-01T10:00:00"))
                .andExpect(jsonPath("$.data.keywords[0]").value("PLANNED"))
                .andExpect(jsonPath("$.data.keywords[1]").value("FOODIE"));

        assertEquals(7L, useCase.command.viewerUserId());
        assertEquals(5L, useCase.command.profileId());
    }

    @Test
    void getsCompanionProfileWithEmptyKeywords() throws Exception {
        useCase.result = result(List.of());

        mockMvc.perform(get("/api/companion-profiles/5")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keywords").isArray())
                .andExpect(jsonPath("$.data.keywords").isEmpty());
    }

    @Test
    void returnsNotFoundForMissingCompanionProfile() throws Exception {
        useCase.exception = new CompanionProfileNotFoundException();

        mockMvc.perform(get("/api/companion-profiles/5")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 프로필을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private CompanionProfileDetail result(final List<TravelStyleKeyword> keywords) {
        return new CompanionProfileDetail(
                5L,
                1L,
                "니어바이",
                UserGender.FEMALE,
                null,
                "https://cdn.nearby.com/profiles/1.jpg",
                "혼자 여행도 같이 여행도 좋아해요",
                new BigDecimal("4.00"),
                12,
                CompanionProfileStatus.ACTIVE,
                LocalDateTime.of(2026, 7, 1, 10, 0),
                keywords,
                List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.PUNCTUAL)
        );
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

    private static final class FakeReadCompanionProfileUseCase implements ReadCompanionProfileUseCase {

        private CompanionProfileDetail result;
        private ReadCompanionProfileCommand command;
        private RuntimeException exception;

        @Override
        public CompanionProfileDetail read(final ReadCompanionProfileCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
