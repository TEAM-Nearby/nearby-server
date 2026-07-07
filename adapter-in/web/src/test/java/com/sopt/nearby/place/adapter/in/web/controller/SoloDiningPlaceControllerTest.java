// 혼밥 맛집 목록 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.place.application.ReadSoloDiningPlacesCommand;
import com.sopt.nearby.place.application.SoloDiningPlacesResult;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.shared.adapter.in.web.exception.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SoloDiningPlaceControllerTest {

    private MockMvc mockMvc;
    private FakeReadSoloDiningPlacesUseCase readUseCase;

    @BeforeEach
    void setUp() {
        readUseCase = new FakeReadSoloDiningPlacesUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SoloDiningPlaceController(readUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsSoloDiningPlaces() throws Exception {
        readUseCase.result = result();

        mockMvc.perform(get("/api/solo-dining/places")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("category", "CAFE")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SOLO_DINING_PLACES_FOUND"))
                .andExpect(jsonPath("$.message").value("혼밥 맛집 목록 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.places[0].placeId").value(12))
                .andExpect(jsonPath("$.data.places[0].googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.places[0].name").value("니어바이 카페"))
                .andExpect(jsonPath("$.data.places[0].photoReference").value("places/google-place-id/photos/photo-resource"))
                .andExpect(jsonPath("$.data.places[0].category").value("CAFE"))
                .andExpect(jsonPath("$.data.places[0].distanceMeters").value(80))
                .andExpect(jsonPath("$.data.places[0].rating").value(4.3))
                .andExpect(jsonPath("$.data.places[0].reviewCount").value(22870))
                .andExpect(jsonPath("$.data.places[0].isFavorite").value(false))
                .andExpect(jsonPath("$.data.places[0].latitude").value(37.56612000))
                .andExpect(jsonPath("$.data.places[0].longitude").value(126.97845000))
                .andExpect(jsonPath("$.data.places[0].businessStatus").value("OPERATIONAL"));

        assertEquals(7L, readUseCase.command.userId());
        assertEquals(new BigDecimal("37.56650000"), readUseCase.command.latitude());
        assertEquals(new BigDecimal("126.97800000"), readUseCase.command.longitude());
        assertEquals(SoloDiningPlaceCategory.CAFE, readUseCase.command.category());
    }

    @Test
    void acceptsMissingCategory() throws Exception {
        readUseCase.result = new SoloDiningPlacesResult(List.of());

        mockMvc.perform(get("/api/solo-dining/places")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.places").isArray())
                .andExpect(jsonPath("$.data.places").isEmpty());

        assertEquals(null, readUseCase.command.category());
    }

    @Test
    void returnsValidationErrorForInvalidQuery() throws Exception {
        mockMvc.perform(get("/api/solo-dining/places")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .queryParam("category", "OTHER")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("위도, 경도, 카테고리 요청값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsGooglePlaceApiError() throws Exception {
        readUseCase.exception = new GooglePlaceApiException();

        mockMvc.perform(get("/api/solo-dining/places")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("GOOGLE_PLACE_API_ERROR"))
                .andExpect(jsonPath("$.message").value("Google Places API 호출에 실패했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private SoloDiningPlacesResult result() {
        return new SoloDiningPlacesResult(List.of(new SoloDiningPlaceSummary(
                12L,
                "google-place-id",
                "니어바이 카페",
                "places/google-place-id/photos/photo-resource",
                SoloDiningPlaceCategory.CAFE,
                80,
                new BigDecimal("4.30"),
                22870,
                false,
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                PlaceBusinessStatus.OPERATIONAL
        )));
    }

    private Principal principal(final String name) {
        return () -> name;
    }

    private static final class FakeReadSoloDiningPlacesUseCase implements ReadSoloDiningPlacesUseCase {

        private ReadSoloDiningPlacesCommand command;
        private SoloDiningPlacesResult result = new SoloDiningPlacesResult(List.of());
        private RuntimeException exception;

        @Override
        public SoloDiningPlacesResult read(final ReadSoloDiningPlacesCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
