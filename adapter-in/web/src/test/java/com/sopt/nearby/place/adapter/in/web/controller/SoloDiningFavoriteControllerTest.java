// 혼밥 맛집 즐겨찾기 목록 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.place.application.ReadSoloDiningFavoritesCommand;
import com.sopt.nearby.place.application.SoloDiningFavoritesResult;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SoloDiningFavoriteControllerTest {

    private MockMvc mockMvc;
    private FakeReadSoloDiningFavoritesUseCase readUseCase;

    @BeforeEach
    void setUp() {
        readUseCase = new FakeReadSoloDiningFavoritesUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SoloDiningFavoriteController(readUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSoloDiningFavorites() throws Exception {
        readUseCase.result = result();

        mockMvc.perform(get("/api/solo-dining/favorites")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("category", "CAFE")
                        .queryParam("sort", "LATEST")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SOLO_DINING_FAVORITES_FOUND"))
                .andExpect(jsonPath("$.message").value("즐겨찾기 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.favorites[0].favoriteId").value(5))
                .andExpect(jsonPath("$.data.favorites[0].createdAt").value("2026-07-02T13:20:00"))
                .andExpect(jsonPath("$.data.favorites[0].placeId").value(12))
                .andExpect(jsonPath("$.data.favorites[0].googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.favorites[0].name").value("니어바이 카페"))
                .andExpect(jsonPath("$.data.favorites[0].photoReference")
                        .value("places/google-place-id/photos/photo-resource"))
                .andExpect(jsonPath("$.data.favorites[0].category").value("CAFE"))
                .andExpect(jsonPath("$.data.favorites[0].distanceMeters").value(80))
                .andExpect(jsonPath("$.data.favorites[0].rating").value(4.3))
                .andExpect(jsonPath("$.data.favorites[0].reviewCount").value(22870))
                .andExpect(jsonPath("$.data.favorites[0].isFavorite").value(true))
                .andExpect(jsonPath("$.data.favorites[0].businessStatus").value("OPERATIONAL"));

        assertEquals(7L, readUseCase.command.userId());
        assertEquals(new BigDecimal("37.56650000"), readUseCase.command.latitude());
        assertEquals(new BigDecimal("126.97800000"), readUseCase.command.longitude());
        assertEquals(SoloDiningPlaceCategory.CAFE, readUseCase.command.category());
        assertEquals(SoloDiningFavoriteSort.LATEST, readUseCase.command.sort());
    }

    @Test
    void usesLatestSortWhenSortIsMissing() throws Exception {
        readUseCase.result = new SoloDiningFavoritesResult(List.of());

        mockMvc.perform(get("/api/solo-dining/favorites")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.favorites").isArray())
                .andExpect(jsonPath("$.data.favorites").isEmpty());

        assertEquals(null, readUseCase.command.category());
        assertEquals(SoloDiningFavoriteSort.LATEST, readUseCase.command.sort());
    }

    @Test
    void returnsValidationErrorForInvalidQuery() throws Exception {
        mockMvc.perform(get("/api/solo-dining/favorites")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("category", "OTHER")
                        .queryParam("sort", "LATEST")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("위도, 경도, 카테고리, 정렬값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));

        mockMvc.perform(get("/api/solo-dining/favorites")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("sort", "DISTANCE")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("위도, 경도, 카테고리, 정렬값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private SoloDiningFavoritesResult result() {
        return new SoloDiningFavoritesResult(List.of(new SoloDiningFavoriteSummary(
                5L,
                LocalDateTime.of(2026, 7, 2, 13, 20),
                12L,
                "google-place-id",
                "니어바이 카페",
                "places/google-place-id/photos/photo-resource",
                SoloDiningPlaceCategory.CAFE,
                80,
                new BigDecimal("4.30"),
                22870,
                true,
                PlaceBusinessStatus.OPERATIONAL
        )));
    }

    private Principal principal(final String name) {
        return () -> name;
    }

    private static final class FakeReadSoloDiningFavoritesUseCase implements ReadSoloDiningFavoritesUseCase {

        private ReadSoloDiningFavoritesCommand command;
        private SoloDiningFavoritesResult result = new SoloDiningFavoritesResult(List.of());

        @Override
        public SoloDiningFavoritesResult read(final ReadSoloDiningFavoritesCommand command) {
            this.command = command;
            return result;
        }
    }
}
