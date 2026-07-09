// 마이페이지 조회 컨트롤러의 요청 전달과 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.ReadMyPageResult;
import com.sopt.nearby.companion.application.ReadMyPageResult.AgeGroup;
import com.sopt.nearby.companion.domain.exception.CompanionProfileNotFoundException;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.in.ReadMyPageUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MyPageControllerTest {

    private MockMvc mockMvc;
    private FakeReadMyPageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadMyPageUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MyPageController(useCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getsMyPage() throws Exception {
        useCase.result = result(AgeGroup.TWENTIES, List.of(TravelStyleKeyword.EXTROVERTED, TravelStyleKeyword.FOODIE));

        mockMvc.perform(get("/api/users/me/mypage")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("READ_MY_PAGE"))
                .andExpect(jsonPath("$.message").value("마이페이지 정보를 조회했어요."))
                .andExpect(jsonPath("$.data.profileImageUrl").value("https://cdn.nearby.com/profiles/1.jpg"))
                .andExpect(jsonPath("$.data.nickname").value("니어바이"))
                .andExpect(jsonPath("$.data.isPhoneVerified").value(true))
                .andExpect(jsonPath("$.data.ageGroup").value("TWENTIES"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.mannerScore").value(4.0))
                .andExpect(jsonPath("$.data.mannerKeywords[0]").value("FAST_RESPONSE"))
                .andExpect(jsonPath("$.data.mannerKeywords[1]").value("PUNCTUAL"))
                .andExpect(jsonPath("$.data.travelStyleKeywords[0]").value("EXTROVERTED"))
                .andExpect(jsonPath("$.data.travelStyleKeywords[1]").value("FOODIE"))
                .andExpect(jsonPath("$.data.mealTogetherCount").value(8))
                .andExpect(jsonPath("$.data.visitedCityCount").value(4))
                .andExpect(jsonPath("$.data.receivedReviewCount").value(12));

        assertEquals(7L, useCase.userId);
    }

    @Test
    void getsMyPageWithNullAgeGroupAndEmptyKeywords() throws Exception {
        useCase.result = result(null, List.of());

        mockMvc.perform(get("/api/users/me/mypage")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.ageGroup").value(nullValue()))
                .andExpect(jsonPath("$.data.travelStyleKeywords").isArray())
                .andExpect(jsonPath("$.data.travelStyleKeywords").isEmpty());
    }

    @Test
    void returnsNotFoundForMissingProfile() throws Exception {
        useCase.exception = new CompanionProfileNotFoundException();

        mockMvc.perform(get("/api/users/me/mypage")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("COMPANION_PROFILE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 프로필을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private ReadMyPageResult result(
            final AgeGroup ageGroup,
            final List<TravelStyleKeyword> travelStyleKeywords
    ) {
        return new ReadMyPageResult(
                "https://cdn.nearby.com/profiles/1.jpg",
                "니어바이",
                true,
                ageGroup,
                UserGender.FEMALE,
                new BigDecimal("4.00"),
                List.of(ReviewKeyword.FAST_RESPONSE, ReviewKeyword.PUNCTUAL),
                travelStyleKeywords,
                8,
                4,
                12
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

    private static final class FakeReadMyPageUseCase implements ReadMyPageUseCase {

        private ReadMyPageResult result;
        private Long userId;
        private RuntimeException exception;

        @Override
        public ReadMyPageResult read(final Long userId) {
            this.userId = userId;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
