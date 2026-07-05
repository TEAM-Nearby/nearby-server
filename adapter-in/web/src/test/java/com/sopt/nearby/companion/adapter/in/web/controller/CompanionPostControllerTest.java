// 동행 모집글 목록 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sopt.nearby.companion.application.NearbyCompanionPostsResult;
import com.sopt.nearby.companion.application.ReadNearbyCompanionPostsCommand;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.in.ReadNearbyCompanionPostsUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import com.sopt.nearby.user.exception.OnboardingRequiredException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CompanionPostControllerTest {

    private MockMvc mockMvc;
    private FakeReadNearbyCompanionPostsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FakeReadNearbyCompanionPostsUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new CompanionPostController(useCase))
                .setMessageConverters(jsonMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void passesDefaultsAndAuthenticatedUserIdToUseCase() throws Exception {
        useCase.result = result(1000, CompanionPostPlaceCategory.ALL, CompanionPostSort.LATEST);

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("COMPANION_POSTS_FOUND"))
                .andExpect(jsonPath("$.message").value("동행 모집 글 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.currentLocation.latitude").value(37.56650000))
                .andExpect(jsonPath("$.data.currentLocation.longitude").value(126.97800000))
                .andExpect(jsonPath("$.data.radiusMeters").value(1000))
                .andExpect(jsonPath("$.data.maxRadiusMeters").value(5000))
                .andExpect(jsonPath("$.data.placeCategory").value("ALL"))
                .andExpect(jsonPath("$.data.sort").value("LATEST"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.summaryText").value("내 주변 1개의 동행이 있어요"))
                .andExpect(jsonPath("$.data.posts[0].postId").value(101))
                .andExpect(jsonPath("$.data.posts[0].status").value("RECRUITING"))
                .andExpect(jsonPath("$.data.posts[0].host.nickname").value("니어바이"))
                .andExpect(jsonPath("$.data.posts[0].host.gender").value("FEMALE"))
                .andExpect(jsonPath("$.data.posts[0].place.placeId").value(20))
                .andExpect(jsonPath("$.data.posts[0].place.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.posts[0].place.name").value("니어바이스시"))
                .andExpect(jsonPath("$.data.posts[0].place.category").value("RESTAURANT"))
                .andExpect(jsonPath("$.data.posts[0].place.distanceMeters").value(320))
                .andExpect(jsonPath("$.data.posts[0].place.imageSource").value("GOOGLE_MAPS"))
                .andExpect(jsonPath("$.data.posts[0].place.imageAttributions").isArray())
                .andExpect(jsonPath("$.data.posts[0].contentPreview").value("같이 스시 먹어요"))
                .andExpect(jsonPath("$.data.posts[0].contentPreviewTruncated").value(false))
                .andExpect(jsonPath("$.data.posts[0].meetingAt").value("2026-07-03T14:00:00"))
                .andExpect(jsonPath("$.data.posts[0].meetingAtText").value("7월 3일 14:00"))
                .andExpect(jsonPath("$.data.posts[0].participantCount").value(2))
                .andExpect(jsonPath("$.data.posts[0].maxParticipants").value(4))
                .andExpect(jsonPath("$.data.posts[0].participantSummaryText").value("2/4 모집 중"))
                .andExpect(jsonPath("$.data.posts[0].createdAt").value("2026-07-02T13:30:00"))
                .andExpect(jsonPath("$.data.posts[0].createdAgoText").value("30분 전"))
                .andExpect(jsonPath("$.data.posts[0].mapMarkerText").value("7월 3일 14시 니어바이스시 동행"));

        assertEquals(7L, useCase.command.userId());
        assertEquals(new BigDecimal("37.56650000"), useCase.command.latitude());
        assertEquals(new BigDecimal("126.97800000"), useCase.command.longitude());
        assertEquals(1000, useCase.command.radiusMeters());
        assertEquals(CompanionPostPlaceCategory.ALL, useCase.command.placeCategory());
        assertEquals(CompanionPostSort.LATEST, useCase.command.sort());
    }

    @Test
    void passesExplicitFiltersToUseCase() throws Exception {
        useCase.result = result(3000, CompanionPostPlaceCategory.CAFE, CompanionPostSort.DISTANCE);

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("radiusMeters", "3000")
                        .queryParam("placeCategory", "CAFE")
                        .queryParam("sort", "DISTANCE")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.radiusMeters").value(3000))
                .andExpect(jsonPath("$.data.placeCategory").value("CAFE"))
                .andExpect(jsonPath("$.data.sort").value("DISTANCE"));

        assertEquals(3000, useCase.command.radiusMeters());
        assertEquals(CompanionPostPlaceCategory.CAFE, useCase.command.placeCategory());
        assertEquals(CompanionPostSort.DISTANCE, useCase.command.sort());
    }

    @Test
    void returnsValidationErrorForInvalidQuery() throws Exception {
        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "invalid")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("위도, 경도, 반경, 카테고리, 정렬 기준이 올바르지 않습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsOnboardingRequired() throws Exception {
        useCase.exception = new OnboardingRequiredException();

        mockMvc.perform(get("/api/companion-posts")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("ONBOARDING_REQUIRED"))
                .andExpect(jsonPath("$.message").value("온보딩 과정이 완료되지 않았습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private NearbyCompanionPostsResult result(
            final int radiusMeters,
            final CompanionPostPlaceCategory placeCategory,
            final CompanionPostSort sort
    ) {
        return new NearbyCompanionPostsResult(
                new NearbyCompanionPostsResult.CurrentLocation(
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000")
                ),
                radiusMeters,
                5000,
                placeCategory,
                sort,
                1,
                "내 주변 1개의 동행이 있어요",
                List.of(new NearbyCompanionPostsResult.Post(
                        101L,
                        CompanionPostStatus.RECRUITING,
                        new NearbyCompanionPostsResult.Host("니어바이", UserGender.FEMALE),
                        new NearbyCompanionPostsResult.Place(
                                20L,
                                "google-place-id",
                                "니어바이스시",
                                CompanionPostPlaceCategory.RESTAURANT,
                                new BigDecimal("37.56710000"),
                                new BigDecimal("126.97920000"),
                                320,
                                "https://lh3.googleusercontent.com/place.jpg",
                                "GOOGLE_MAPS",
                                List.of()
                        ),
                        "같이 스시 먹어요",
                        false,
                        LocalDateTime.of(2026, 7, 3, 14, 0),
                        "7월 3일 14:00",
                        2,
                        4,
                        "2/4 모집 중",
                        LocalDateTime.of(2026, 7, 2, 13, 30),
                        "30분 전",
                        "7월 3일 14시 니어바이스시 동행"
                ))
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

    private static final class FakeReadNearbyCompanionPostsUseCase implements ReadNearbyCompanionPostsUseCase {

        private NearbyCompanionPostsResult result;
        private ReadNearbyCompanionPostsCommand command;
        private RuntimeException exception;

        @Override
        public NearbyCompanionPostsResult read(final ReadNearbyCompanionPostsCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
