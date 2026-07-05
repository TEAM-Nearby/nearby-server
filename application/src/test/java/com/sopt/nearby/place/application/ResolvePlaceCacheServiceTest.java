// 장소 캐시 조회 저장 서비스의 중복 저장 충돌 복구를 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ResolvePlaceCacheServiceTest {

    @Test
    void returnsExistingPlaceWithoutSavingWhenGooglePlaceIdAlreadyExists() {
        FakePlaceCacheRepository placeCacheRepository = new FakePlaceCacheRepository();
        ResolvePlaceCacheService service = new ResolvePlaceCacheService(placeCacheRepository);
        ResolvePlaceCacheCommand command = command();
        placeCacheRepository.places.put(command.googlePlaceId(), place(7L, command.googlePlaceId()));

        ResolvedPlaceCache result = service.resolve(command);

        assertEquals(7L, result.placeId());
        assertEquals(1, placeCacheRepository.findAttempts);
        assertEquals(0, placeCacheRepository.saveAttempts);
    }

    @Test
    void savesNewPlaceWhenGooglePlaceIdDoesNotExist() {
        FakePlaceCacheRepository placeCacheRepository = new FakePlaceCacheRepository();
        ResolvePlaceCacheService service = new ResolvePlaceCacheService(placeCacheRepository);
        ResolvePlaceCacheCommand command = command();

        ResolvedPlaceCache result = service.resolve(command);

        assertEquals(1L, result.placeId());
        assertEquals(1, placeCacheRepository.findAttempts);
        assertEquals(1, placeCacheRepository.saveAttempts);
        assertEquals(1L, placeCacheRepository.places.get(command.googlePlaceId()).id());
    }

    @Test
    void reloadsExistingPlaceWhenConcurrentSaveCreatesUniqueConstraintViolation() {
        FakePlaceCacheRepository placeCacheRepository = new FakePlaceCacheRepository();
        ResolvePlaceCacheService service = new ResolvePlaceCacheService(placeCacheRepository);
        ResolvePlaceCacheCommand command = command();
        placeCacheRepository.saveException = new DataIntegrityViolationException("duplicate google_place_id");
        placeCacheRepository.existingAfterConflict = place(1L, command.googlePlaceId());

        ResolvedPlaceCache result = service.resolve(command);

        assertEquals(1L, result.placeId());
        assertEquals(1, placeCacheRepository.saveAttempts);
        assertEquals(2, placeCacheRepository.findAttempts);
    }

    private ResolvePlaceCacheCommand command() {
        return new ResolvePlaceCacheCommand(
                "google-place-id",
                "Siutat condal",
                "Rambla de Catalunya, 16",
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800")
        );
    }

    private PlaceCache place(final Long id, final String googlePlaceId) {
        return new PlaceCache(
                id,
                googlePlaceId,
                "Siutat condal",
                "Rambla de Catalunya, 16",
                new BigDecimal("41.39020500"),
                new BigDecimal("2.16354800"),
                null,
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        );
    }

    private static final class FakePlaceCacheRepository implements PlaceCacheRepository {

        private final Map<String, PlaceCache> places = new HashMap<>();
        private DataIntegrityViolationException saveException;
        private PlaceCache existingAfterConflict;
        private int findAttempts;
        private int saveAttempts;

        @Override
        public PlaceCache save(final PlaceCache model) {
            saveAttempts++;
            if (saveException != null) {
                places.put(existingAfterConflict.googlePlaceId(), existingAfterConflict);
                throw saveException;
            }
            PlaceCache saved = model.id() == null ? withId(model, places.size() + 1L) : model;
            places.put(saved.googlePlaceId(), saved);
            return saved;
        }

        @Override
        public Optional<PlaceCache> findById(final Long id) {
            return places.values()
                    .stream()
                    .filter(place -> place.id().equals(id))
                    .findFirst();
        }

        @Override
        public Optional<PlaceCache> findByGooglePlaceId(final String googlePlaceId) {
            findAttempts++;
            return Optional.ofNullable(places.get(googlePlaceId));
        }

        private PlaceCache withId(final PlaceCache model, final Long id) {
            return new PlaceCache(
                    id,
                    model.googlePlaceId(),
                    model.name(),
                    model.address(),
                    model.latitude(),
                    model.longitude(),
                    model.category(),
                    model.phoneNumber(),
                    model.rating(),
                    model.reviewCount(),
                    model.photoReference(),
                    model.businessStatus()
            );
        }
    }
}
