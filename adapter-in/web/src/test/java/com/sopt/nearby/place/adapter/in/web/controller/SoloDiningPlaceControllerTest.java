// 혼밥 맛집 목록 조회 컨트롤러의 요청 파싱과 응답 형식을 검증한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sopt.nearby.place.application.ReadSoloDiningPlacesCommand;
import com.sopt.nearby.place.application.ReadSoloDiningPlaceCommand;
import com.sopt.nearby.place.application.SoloDiningFavoriteCommand;
import com.sopt.nearby.place.application.SoloDiningFavoriteResult;
import com.sopt.nearby.place.application.SoloDiningPlaceResult;
import com.sopt.nearby.place.application.SoloDiningPlacesResult;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ManageSoloDiningFavoriteUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
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
    private FakeReadSoloDiningPlaceUseCase readDetailUseCase;
    private FakeManageSoloDiningFavoriteUseCase favoriteUseCase;

    @BeforeEach
    void setUp() {
        readUseCase = new FakeReadSoloDiningPlacesUseCase();
        readDetailUseCase = new FakeReadSoloDiningPlaceUseCase();
        favoriteUseCase = new FakeManageSoloDiningFavoriteUseCase();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SoloDiningPlaceController(readUseCase, readDetailUseCase, favoriteUseCase))
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

    @Test
    void returnsSoloDiningPlace() throws Exception {
        readDetailUseCase.result = detailResult();

        mockMvc.perform(get("/api/solo-dining/places/12")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SOLO_DINING_PLACE_FOUND"))
                .andExpect(jsonPath("$.message").value("혼밥 맛집 상세 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.placeId").value(12))
                .andExpect(jsonPath("$.data.googlePlaceId").value("google-place-id"))
                .andExpect(jsonPath("$.data.name").value("르상크 바르셀로나점"))
                .andExpect(jsonPath("$.data.address").value("서울특별시 중구 세종대로 110"))
                .andExpect(jsonPath("$.data.latitude").value(37.56612000))
                .andExpect(jsonPath("$.data.longitude").value(126.97845000))
                .andExpect(jsonPath("$.data.category").value("CAFE"))
                .andExpect(jsonPath("$.data.distanceMeters").value(80))
                .andExpect(jsonPath("$.data.rating").value(4.3))
                .andExpect(jsonPath("$.data.reviewCount").value(22870))
                .andExpect(jsonPath("$.data.phoneNumber").value("02-1234-5678"))
                .andExpect(jsonPath("$.data.photoReference").value("places/google-place-id/photos/photo-1"))
                .andExpect(jsonPath("$.data.photoReferences[0]").value("places/google-place-id/photos/photo-1"))
                .andExpect(jsonPath("$.data.businessStatus").value("OPERATIONAL"))
                .andExpect(jsonPath("$.data.priceLevel").value("PRICE_LEVEL_MODERATE"))
                .andExpect(jsonPath("$.data.priceRange").value("₩10,000~₩20,000"))
                .andExpect(jsonPath("$.data.regularOpeningHours[0]").value("월요일: 오전 11:00~오후 9:00"))
                .andExpect(jsonPath("$.data.editorialSummary").value("혼밥하기 좋은 조용한 식당입니다."))
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        assertEquals(7L, readDetailUseCase.command.userId());
        assertEquals(12L, readDetailUseCase.command.placeId());
        assertEquals(new BigDecimal("37.56650000"), readDetailUseCase.command.latitude());
        assertEquals(new BigDecimal("126.97800000"), readDetailUseCase.command.longitude());
    }

    @Test
    void returnsValidationErrorForInvalidDetailRequest() throws Exception {
        mockMvc.perform(get("/api/solo-dining/places/0")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("placeId, 위도, 경도 요청값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsNotFoundForMissingSoloDiningPlace() throws Exception {
        readDetailUseCase.exception = new PlaceNotFoundException();

        mockMvc.perform(get("/api/solo-dining/places/12")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsGooglePlaceApiErrorForDetail() throws Exception {
        readDetailUseCase.exception = new GooglePlaceApiException();

        mockMvc.perform(get("/api/solo-dining/places/12")
                        .queryParam("latitude", "37.56650000")
                        .queryParam("longitude", "126.97800000")
                        .principal(principal("7")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.code").value("GOOGLE_PLACE_API_ERROR"))
                .andExpect(jsonPath("$.message").value("Google Places API 호출에 실패했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void registersSoloDiningFavorite() throws Exception {
        favoriteUseCase.result = new SoloDiningFavoriteResult(true);

        mockMvc.perform(put("/api/solo-dining/places/12/favorite")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SOLO_DINING_FAVORITE_REGISTERED"))
                .andExpect(jsonPath("$.message").value("식당 즐겨찾기 등록에 성공했습니다."))
                .andExpect(jsonPath("$.data.isFavorite").value(true));

        assertEquals(7L, favoriteUseCase.registerCommand.userId());
        assertEquals(12L, favoriteUseCase.registerCommand.placeId());
    }

    @Test
    void removesSoloDiningFavorite() throws Exception {
        favoriteUseCase.result = new SoloDiningFavoriteResult(false);

        mockMvc.perform(delete("/api/solo-dining/places/12/favorite")
                        .principal(principal("7")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.code").value("SOLO_DINING_FAVORITE_REMOVED"))
                .andExpect(jsonPath("$.message").value("식당 즐겨찾기 해제에 성공했습니다."))
                .andExpect(jsonPath("$.data.isFavorite").value(false));

        assertEquals(7L, favoriteUseCase.removeCommand.userId());
        assertEquals(12L, favoriteUseCase.removeCommand.placeId());
    }

    @Test
    void returnsValidationErrorForInvalidFavoriteRequest() throws Exception {
        mockMvc.perform(put("/api/solo-dining/places/0/favorite")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("placeId 요청값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsNotFoundForMissingFavoritePlace() throws Exception {
        favoriteUseCase.exception = new PlaceNotFoundException();

        mockMvc.perform(put("/api/solo-dining/places/12/favorite")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsValidationErrorForInvalidRemoveFavoriteRequest() throws Exception {
        mockMvc.perform(delete("/api/solo-dining/places/0/favorite")
                        .principal(principal("7")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("placeId 요청값 오류가 발생했습니다."))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void returnsNotFoundForMissingFavoritePlaceOnRemove() throws Exception {
        favoriteUseCase.exception = new PlaceNotFoundException();

        mockMvc.perform(delete("/api/solo-dining/places/12/favorite")
                        .principal(principal("7")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("PLACE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("장소를 찾을 수 없습니다."))
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

    private SoloDiningPlaceResult detailResult() {
        return new SoloDiningPlaceResult(
                12L,
                "google-place-id",
                "르상크 바르셀로나점",
                "서울특별시 중구 세종대로 110",
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                SoloDiningPlaceCategory.CAFE,
                80,
                new BigDecimal("4.30"),
                22870,
                "02-1234-5678",
                "places/google-place-id/photos/photo-1",
                List.of("places/google-place-id/photos/photo-1"),
                PlaceBusinessStatus.OPERATIONAL,
                "PRICE_LEVEL_MODERATE",
                "₩10,000~₩20,000",
                List.of("월요일: 오전 11:00~오후 9:00"),
                "혼밥하기 좋은 조용한 식당입니다.",
                true
        );
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

    private static final class FakeReadSoloDiningPlaceUseCase implements ReadSoloDiningPlaceUseCase {

        private ReadSoloDiningPlaceCommand command;
        private SoloDiningPlaceResult result;
        private RuntimeException exception;

        @Override
        public SoloDiningPlaceResult read(final ReadSoloDiningPlaceCommand command) {
            this.command = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }

    private static final class FakeManageSoloDiningFavoriteUseCase implements ManageSoloDiningFavoriteUseCase {

        private SoloDiningFavoriteCommand registerCommand;
        private SoloDiningFavoriteCommand removeCommand;
        private SoloDiningFavoriteResult result;
        private RuntimeException exception;

        @Override
        public SoloDiningFavoriteResult register(final SoloDiningFavoriteCommand command) {
            this.registerCommand = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }

        @Override
        public SoloDiningFavoriteResult remove(final SoloDiningFavoriteCommand command) {
            this.removeCommand = command;
            if (exception != null) {
                throw exception;
            }
            return result;
        }
    }
}
