// 혼밥 맛집 상세 조회 서비스의 캐시 확인, Google 상세 조회, 응답 조립을 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlaceRequestException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadSoloDiningPlaceServiceTest {

    private FakeSoloDiningPlaceQueryPort queryPort;
    private FakeSoloDiningPlaceDetailsPort detailsPort;
    private FakeResolvePlaceImageUseCase resolvePlaceImageUseCase;
    private ReadSoloDiningPlaceService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeSoloDiningPlaceQueryPort();
        detailsPort = new FakeSoloDiningPlaceDetailsPort();
        resolvePlaceImageUseCase = new FakeResolvePlaceImageUseCase();
        service = new ReadSoloDiningPlaceService(queryPort, detailsPort, resolvePlaceImageUseCase);
    }

    @Test
    void readsCachedPlaceThenGoogleDetails() {
        queryPort.result = List.of(summary());
        detailsPort.result = details();
        resolvePlaceImageUseCase.result = new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/place.jpg",
                ResolvedPlaceImage.GOOGLE_MAPS,
                List.of()
        );

        SoloDiningPlaceResult result = service.read(validCommand());

        assertEquals(7L, queryPort.userId);
        assertEquals(new BigDecimal("37.56650000"), queryPort.latitude);
        assertEquals(new BigDecimal("126.97800000"), queryPort.longitude);
        assertEquals(List.of(12L), queryPort.placeIds);
        assertEquals("google-place-id", detailsPort.googlePlaceId);
        assertEquals(12L, result.placeId());
        assertEquals("google-place-id", result.googlePlaceId());
        assertEquals("르상크 바르셀로나점", result.name());
        assertEquals("서울특별시 중구 세종대로 110", result.address());
        assertEquals(new BigDecimal("37.56612000"), result.latitude());
        assertEquals(new BigDecimal("126.97845000"), result.longitude());
        assertEquals(SoloDiningPlaceCategory.CAFE, result.category());
        assertEquals(80, result.distanceMeters());
        assertEquals(new BigDecimal("4.30"), result.rating());
        assertEquals(22870, result.reviewCount());
        assertEquals("02-1234-5678", result.phoneNumber());
        assertEquals("places/google-place-id/photos/photo-1", result.photoReference());
        assertEquals(List.of(
                "places/google-place-id/photos/photo-1",
                "places/google-place-id/photos/photo-2"
        ), result.photoReferences());
        assertEquals("google-place-id", resolvePlaceImageUseCase.command.googlePlaceId());
        assertEquals("places/google-place-id/photos/photo-1", resolvePlaceImageUseCase.command.photoReference());
        assertEquals("https://lh3.googleusercontent.com/place.jpg", result.imageUrl());
        assertEquals(PlaceBusinessStatus.OPERATIONAL, result.businessStatus());
        assertEquals("PRICE_LEVEL_MODERATE", result.priceLevel());
        assertEquals("₩10,000~₩20,000", result.priceRange());
        assertEquals(List.of("월요일: 오전 11:00~오후 9:00"), result.regularOpeningHours());
        assertEquals("혼밥하기 좋은 조용한 식당입니다.", result.editorialSummary());
        assertEquals(true, result.isFavorite());
    }

    @Test
    void resolvesCachedPhotoWhenGoogleDetailsPhotoIsMissing() {
        queryPort.result = List.of(summary());
        detailsPort.result = detailsWithoutPhoto();
        resolvePlaceImageUseCase.result = new ResolvedPlaceImage(
                "https://lh3.googleusercontent.com/cached-place.jpg",
                ResolvedPlaceImage.GOOGLE_MAPS,
                List.of()
        );

        SoloDiningPlaceResult result = service.read(validCommand());

        assertEquals("places/google-place-id/photos/cached-photo", result.photoReference());
        assertEquals("places/google-place-id/photos/cached-photo", resolvePlaceImageUseCase.command.photoReference());
        assertEquals("https://lh3.googleusercontent.com/cached-place.jpg", result.imageUrl());
    }

    @Test
    void returnsDefaultImageWhenImageResolutionFallsBack() {
        queryPort.result = List.of(summary());
        detailsPort.result = details();

        SoloDiningPlaceResult result = service.read(validCommand());

        assertEquals("https://nearby.sopt.org/images/default-place.png", result.imageUrl());
    }

    @Test
    void returnsNullCategoryWhenGoogleDetailsCategoryIsMissing() {
        queryPort.result = List.of(summary());
        detailsPort.result = new SoloDiningPlaceDetailsResult(
                "google-place-id",
                "르상크 바르셀로나점",
                "서울특별시 중구 세종대로 110",
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                null,
                new BigDecimal("4.30"),
                22870,
                "02-1234-5678",
                "places/google-place-id/photos/photo-1",
                List.of("places/google-place-id/photos/photo-1"),
                PlaceBusinessStatus.OPERATIONAL,
                "PRICE_LEVEL_MODERATE",
                "₩10,000~₩20,000",
                List.of("월요일: 오전 11:00~오후 9:00"),
                "혼밥하기 좋은 조용한 식당입니다."
        );

        SoloDiningPlaceResult result = service.read(validCommand());

        assertNull(result.category());
    }

    @Test
    void throwsNotFoundWhenCachedPlaceDoesNotExist() {
        queryPort.result = List.of();

        assertThrows(PlaceNotFoundException.class, () -> service.read(validCommand()));
        assertNull(detailsPort.googlePlaceId);
    }

    @Test
    void throwsGooglePlaceApiExceptionWhenDetailsAreMissing() {
        queryPort.result = List.of(summary());
        detailsPort.result = null;

        assertThrows(GooglePlaceApiException.class, () -> service.read(validCommand()));
    }

    @Test
    void rejectsInvalidCommandBeforeDependencies() {
        assertThrows(InvalidSoloDiningPlaceRequestException.class, () -> service.read(new ReadSoloDiningPlaceCommand(
                7L,
                0L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000")
        )));
        assertThrows(InvalidSoloDiningPlaceRequestException.class, () -> service.read(new ReadSoloDiningPlaceCommand(
                7L,
                12L,
                new BigDecimal("91.00000000"),
                new BigDecimal("126.97800000")
        )));
        assertNull(queryPort.userId);
    }

    private ReadSoloDiningPlaceCommand validCommand() {
        return new ReadSoloDiningPlaceCommand(
                7L,
                12L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000")
        );
    }

    private SoloDiningPlaceSummary summary() {
        return new SoloDiningPlaceSummary(
                12L,
                "google-place-id",
                "캐시 이름",
                "캐시 주소",
                "places/google-place-id/photos/cached-photo",
                SoloDiningPlaceCategory.RESTAURANT,
                80,
                new BigDecimal("4.10"),
                100,
                true,
                new BigDecimal("37.56600000"),
                new BigDecimal("126.97800000"),
                PlaceBusinessStatus.UNKNOWN
        );
    }

    private SoloDiningPlaceDetailsResult details() {
        return new SoloDiningPlaceDetailsResult(
                "google-place-id",
                "르상크 바르셀로나점",
                "서울특별시 중구 세종대로 110",
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                SoloDiningPlaceCategory.CAFE,
                new BigDecimal("4.30"),
                22870,
                "02-1234-5678",
                "places/google-place-id/photos/photo-1",
                List.of(
                        "places/google-place-id/photos/photo-1",
                        "places/google-place-id/photos/photo-2"
                ),
                PlaceBusinessStatus.OPERATIONAL,
                "PRICE_LEVEL_MODERATE",
                "₩10,000~₩20,000",
                List.of("월요일: 오전 11:00~오후 9:00"),
                "혼밥하기 좋은 조용한 식당입니다."
        );
    }

    private SoloDiningPlaceDetailsResult detailsWithoutPhoto() {
        SoloDiningPlaceDetailsResult details = details();
        return new SoloDiningPlaceDetailsResult(
                details.googlePlaceId(),
                details.name(),
                details.address(),
                details.latitude(),
                details.longitude(),
                details.category(),
                details.rating(),
                details.reviewCount(),
                details.phoneNumber(),
                null,
                List.of(),
                details.businessStatus(),
                details.priceLevel(),
                details.priceRange(),
                details.regularOpeningHours(),
                details.editorialSummary()
        );
    }

    private static final class FakeSoloDiningPlaceQueryPort implements SoloDiningPlaceQueryPort {

        private Long userId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private List<Long> placeIds;
        private List<SoloDiningPlaceSummary> result = List.of();

        @Override
        public List<SoloDiningPlaceSummary> findAllNearby(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final SoloDiningPlaceCategory category,
                final int radiusMeters
        ) {
            return List.of();
        }

        @Override
        public List<SoloDiningPlaceSummary> findAllByPlaceIds(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final List<Long> placeIds
        ) {
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.placeIds = placeIds;
            return result;
        }
    }

    private static final class FakeSoloDiningPlaceDetailsPort implements SoloDiningPlaceDetailsPort {

        private String googlePlaceId;
        private SoloDiningPlaceDetailsResult result;

        @Override
        public SoloDiningPlaceDetailsResult findByGooglePlaceId(final String googlePlaceId) {
            this.googlePlaceId = googlePlaceId;
            return result;
        }
    }

    private static final class FakeResolvePlaceImageUseCase implements ResolvePlaceImageUseCase {

        private ResolvePlaceImageCommand command;
        private ResolvedPlaceImage result = new ResolvedPlaceImage(
                "https://nearby.sopt.org/images/default-place.png",
                ResolvedPlaceImage.DEFAULT,
                List.of()
        );

        @Override
        public ResolvedPlaceImage resolve(final ResolvePlaceImageCommand command) {
            this.command = command;
            return result;
        }
    }
}
