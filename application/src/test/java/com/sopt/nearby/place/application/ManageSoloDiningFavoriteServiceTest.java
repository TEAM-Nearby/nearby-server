// 혼밥 맛집 즐겨찾기 등록과 해제 유스케이스를 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.DuplicateSoloDiningFavoriteException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoriteRequestException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ManageSoloDiningFavoriteServiceTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-17T09:00:00Z"),
            ZoneOffset.UTC
    );

    private FakePlaceCacheRepository placeCacheRepository;
    private FakeSoloDiningFavoriteRepository favoriteRepository;
    private ManageSoloDiningFavoriteService service;

    @BeforeEach
    void setUp() {
        placeCacheRepository = new FakePlaceCacheRepository();
        favoriteRepository = new FakeSoloDiningFavoriteRepository();
        service = new ManageSoloDiningFavoriteService(placeCacheRepository, favoriteRepository, CLOCK);
    }

    @Test
    void registersFavoriteWhenPlaceExistsAndFavoriteDoesNotExist() {
        placeCacheRepository.place = place();

        SoloDiningFavoriteResult result = service.register(new SoloDiningFavoriteCommand(7L, 12L));

        assertEquals(true, result.isFavorite());
        assertEquals(7L, favoriteRepository.saved.userId());
        assertEquals(12L, favoriteRepository.saved.placeId());
        assertEquals(LocalDateTime.of(2026, 7, 17, 9, 0), favoriteRepository.saved.createdAt());
    }

    @Test
    void returnsFavoriteWhenAlreadyRegistered() {
        placeCacheRepository.place = place();
        favoriteRepository.favorite = new SoloDiningFavorite(5L, 7L, 12L, LocalDateTime.now());

        SoloDiningFavoriteResult result = service.register(new SoloDiningFavoriteCommand(7L, 12L));

        assertEquals(true, result.isFavorite());
        assertEquals(null, favoriteRepository.saved);
    }

    @Test
    void returnsFavoriteWhenConcurrentSaveCreatesDuplicateFavorite() {
        placeCacheRepository.place = place();
        favoriteRepository.saveException = new DuplicateSoloDiningFavoriteException(new RuntimeException());

        SoloDiningFavoriteResult result = service.register(new SoloDiningFavoriteCommand(7L, 12L));

        assertEquals(true, result.isFavorite());
        assertEquals(7L, favoriteRepository.saved.userId());
        assertEquals(12L, favoriteRepository.saved.placeId());
    }

    @Test
    void removesFavoriteWhenPlaceExists() {
        placeCacheRepository.place = place();
        favoriteRepository.favorite = new SoloDiningFavorite(5L, 7L, 12L, LocalDateTime.now());

        SoloDiningFavoriteResult result = service.remove(new SoloDiningFavoriteCommand(7L, 12L));

        assertEquals(false, result.isFavorite());
        assertEquals(7L, favoriteRepository.deletedUserId);
        assertEquals(12L, favoriteRepository.deletedPlaceId);
        assertEquals(null, favoriteRepository.favorite);
    }

    @Test
    void returnsRemovedWhenFavoriteDoesNotExist() {
        placeCacheRepository.place = place();

        SoloDiningFavoriteResult result = service.remove(new SoloDiningFavoriteCommand(7L, 12L));

        assertEquals(false, result.isFavorite());
        assertEquals(7L, favoriteRepository.deletedUserId);
        assertEquals(12L, favoriteRepository.deletedPlaceId);
    }

    @Test
    void throwsPlaceNotFoundWhenPlaceDoesNotExist() {
        assertThrows(PlaceNotFoundException.class,
                () -> service.register(new SoloDiningFavoriteCommand(7L, 12L)));
        assertThrows(PlaceNotFoundException.class,
                () -> service.remove(new SoloDiningFavoriteCommand(7L, 12L)));
    }

    @Test
    void rejectsInvalidCommandBeforeDependencies() {
        assertThrows(InvalidSoloDiningFavoriteRequestException.class,
                () -> service.register(new SoloDiningFavoriteCommand(7L, 0L)));
        assertThrows(InvalidSoloDiningFavoriteRequestException.class,
                () -> service.remove(new SoloDiningFavoriteCommand(7L, 0L)));
        assertEquals(null, placeCacheRepository.findById);
    }

    private PlaceCache place() {
        return new PlaceCache(
                12L,
                "google-place-id",
                "니어바이 카페",
                "서울특별시 중구 세종대로 110",
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                "CAFE",
                null,
                new BigDecimal("4.30"),
                22870,
                "places/google-place-id/photos/photo-resource",
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private static final class FakePlaceCacheRepository implements PlaceCacheRepository {

        private Long findById;
        private PlaceCache place;

        @Override
        public PlaceCache save(final PlaceCache model) {
            return model;
        }

        @Override
        public Optional<PlaceCache> findById(final Long id) {
            this.findById = id;
            return Optional.ofNullable(place);
        }

        @Override
        public Optional<PlaceCache> findByGooglePlaceId(final String googlePlaceId) {
            return Optional.empty();
        }
    }

    private static final class FakeSoloDiningFavoriteRepository implements SoloDiningFavoriteRepository {

        private SoloDiningFavorite favorite;
        private SoloDiningFavorite saved;
        private RuntimeException saveException;
        private Long deletedUserId;
        private Long deletedPlaceId;

        @Override
        public SoloDiningFavorite save(final SoloDiningFavorite model) {
            this.saved = model;
            if (saveException != null) {
                throw saveException;
            }
            return new SoloDiningFavorite(5L, model.userId(), model.placeId(), model.createdAt());
        }

        @Override
        public Optional<SoloDiningFavorite> findById(final Long id) {
            return Optional.empty();
        }

        @Override
        public Optional<SoloDiningFavorite> findByUserIdAndPlaceId(final Long userId, final Long placeId) {
            return Optional.ofNullable(favorite);
        }

        @Override
        public void deleteByUserIdAndPlaceId(final Long userId, final Long placeId) {
            this.deletedUserId = userId;
            this.deletedPlaceId = placeId;
            if (favorite != null && favorite.userId().equals(userId) && favorite.placeId().equals(placeId)) {
                favorite = null;
            }
        }
    }
}
