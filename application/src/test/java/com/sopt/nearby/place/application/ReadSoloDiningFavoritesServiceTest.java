// 혼밥 맛집 즐겨찾기 목록 조회 유스케이스를 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadSoloDiningFavoritesServiceTest {

    private FakeSoloDiningFavoriteQueryPort queryPort;
    private ReadSoloDiningFavoritesService service;

    @BeforeEach
    void setUp() {
        queryPort = new FakeSoloDiningFavoriteQueryPort();
        service = new ReadSoloDiningFavoritesService(queryPort);
    }

    @Test
    void returnsFavoritesFromQueryPort() {
        queryPort.result = List.of(summary(5L, SoloDiningPlaceCategory.CAFE));

        SoloDiningFavoritesResult result = service.read(validCommand(SoloDiningPlaceCategory.CAFE));

        assertEquals(7L, queryPort.userId);
        assertEquals(new BigDecimal("37.56650000"), queryPort.latitude);
        assertEquals(new BigDecimal("126.97800000"), queryPort.longitude);
        assertEquals(SoloDiningPlaceCategory.CAFE, queryPort.category);
        assertEquals(SoloDiningFavoriteSort.LATEST, queryPort.sort);
        assertEquals(1, result.favorites().size());
        assertEquals(5L, result.favorites().get(0).favoriteId());
        assertEquals(true, result.favorites().get(0).isFavorite());
    }

    @Test
    void acceptsMissingCategory() {
        queryPort.result = List.of(summary(5L, null));

        SoloDiningFavoritesResult result = service.read(validCommand(null));

        assertEquals(null, queryPort.category);
        assertEquals(1, result.favorites().size());
        assertEquals(null, result.favorites().get(0).category());
    }

    @Test
    void rejectsInvalidCommand() {
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        null,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        null,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("91.00000000"),
                        new BigDecimal("126.97800000"),
                        null,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        SoloDiningPlaceCategory.OTHER,
                        SoloDiningFavoriteSort.LATEST
                )));
        assertThrows(InvalidSoloDiningFavoritesRequestException.class,
                () -> service.read(new ReadSoloDiningFavoritesCommand(
                        7L,
                        new BigDecimal("37.56650000"),
                        new BigDecimal("126.97800000"),
                        null,
                        null
                )));
    }

    private ReadSoloDiningFavoritesCommand validCommand(final SoloDiningPlaceCategory category) {
        return new ReadSoloDiningFavoritesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                category,
                SoloDiningFavoriteSort.LATEST
        );
    }

    private SoloDiningFavoriteSummary summary(
            final Long favoriteId,
            final SoloDiningPlaceCategory category
    ) {
        return new SoloDiningFavoriteSummary(
                favoriteId,
                LocalDateTime.of(2026, 7, 2, 13, 20),
                12L,
                "google-place-id",
                "니어바이 카페",
                "서울특별시 중구 세종대로 110",
                "places/google-place-id/photos/photo-resource",
                category,
                80,
                new BigDecimal("4.30"),
                22870,
                true,
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private static final class FakeSoloDiningFavoriteQueryPort implements SoloDiningFavoriteQueryPort {

        private Long userId;
        private BigDecimal latitude;
        private BigDecimal longitude;
        private SoloDiningPlaceCategory category;
        private SoloDiningFavoriteSort sort;
        private List<SoloDiningFavoriteSummary> result = List.of();

        @Override
        public List<SoloDiningFavoriteSummary> findAllByUserId(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final SoloDiningPlaceCategory category,
                final SoloDiningFavoriteSort sort
        ) {
            this.userId = userId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.category = category;
            this.sort = sort;
            return result;
        }
    }
}
