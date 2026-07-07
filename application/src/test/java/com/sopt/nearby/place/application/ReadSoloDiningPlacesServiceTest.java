// 혼밥 맛집 목록 조회 서비스의 검색, 캐시 저장, 응답 조립을 검증한다.
package com.sopt.nearby.place.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sopt.nearby.place.domain.exception.DuplicatePlaceCacheException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReadSoloDiningPlacesServiceTest {

    private FakeSoloDiningPlaceSearchPort searchPort;
    private FakePlaceCacheRepository placeCacheRepository;
    private FakeSoloDiningPlaceQueryPort queryPort;
    private ReadSoloDiningPlacesService service;

    @BeforeEach
    void setUp() {
        searchPort = new FakeSoloDiningPlaceSearchPort();
        placeCacheRepository = new FakePlaceCacheRepository();
        queryPort = new FakeSoloDiningPlaceQueryPort();
        service = new ReadSoloDiningPlacesService(searchPort, placeCacheRepository, queryPort);
    }

    @Test
    void searchesGoogleCachesPlacesAndReturnsFavoriteAwarePlaces() {
        searchPort.result = List.of(searchResult("google-place-id", "니어바이 카페", SoloDiningPlaceCategory.CAFE));
        queryPort.result = List.of(summary(1L, true));

        SoloDiningPlacesResult result = service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                SoloDiningPlaceCategory.CAFE
        ));

        assertEquals(new BigDecimal("37.56650000"), searchPort.request.latitude());
        assertEquals(new BigDecimal("126.97800000"), searchPort.request.longitude());
        assertEquals(1000, searchPort.request.radiusMeters());
        assertEquals(20, searchPort.request.maxResultCount());
        assertEquals(List.of("cafe"), searchPort.request.includedTypes());
        assertEquals("google-place-id", placeCacheRepository.saved.googlePlaceId());
        assertEquals("니어바이 카페", placeCacheRepository.saved.name());
        assertEquals(new BigDecimal("4.30"), placeCacheRepository.saved.rating());
        assertEquals(7L, queryPort.userId);
        assertEquals(List.of(1L), queryPort.placeIds);
        assertEquals(1, result.places().size());
        assertEquals("니어바이 카페", result.places().get(0).name());
        assertEquals(true, result.places().get(0).isFavorite());
    }

    @Test
    void searchesAllSoloDiningTypesWhenCategoryIsMissing() {
        service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                null
        ));

        assertEquals(List.of("restaurant", "cafe", "pub"), searchPort.request.includedTypes());
    }

    @Test
    void updatesExistingCacheInsteadOfCreatingDuplicate() {
        placeCacheRepository.places.put("google-place-id", new PlaceCache(
                9L,
                "google-place-id",
                "예전 이름",
                "예전 주소",
                new BigDecimal("37.00000000"),
                new BigDecimal("126.00000000"),
                "RESTAURANT",
                "02-1234-5678",
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        ));
        searchPort.result = List.of(searchResult("google-place-id", "새 이름", SoloDiningPlaceCategory.RESTAURANT));
        queryPort.result = List.of(summary(9L, false));

        service.read(validCommand());

        assertEquals(9L, placeCacheRepository.saved.id());
        assertEquals("새 이름", placeCacheRepository.saved.name());
        assertEquals("02-1234-5678", placeCacheRepository.saved.phoneNumber());
        assertEquals(List.of(9L), queryPort.placeIds);
    }

    @Test
    void preservesMeaningfulExistingCategoryWhenUpdatingCache() {
        placeCacheRepository.places.put("google-place-id", new PlaceCache(
                9L,
                "google-place-id",
                "예전 이름",
                "예전 주소",
                new BigDecimal("37.00000000"),
                new BigDecimal("126.00000000"),
                "MUSEUM",
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        ));
        searchPort.result = List.of(searchResult("google-place-id", "새 이름", SoloDiningPlaceCategory.CAFE));
        queryPort.result = List.of(summary(9L, false));

        service.read(validCommand());

        assertEquals("MUSEUM", placeCacheRepository.saved.category());
    }

    @Test
    void replacesOtherExistingCategoryWithGoogleCategoryWhenUpdatingCache() {
        placeCacheRepository.places.put("google-place-id", new PlaceCache(
                9L,
                "google-place-id",
                "예전 이름",
                "예전 주소",
                new BigDecimal("37.00000000"),
                new BigDecimal("126.00000000"),
                "OTHER",
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        ));
        searchPort.result = List.of(searchResult("google-place-id", "새 이름", SoloDiningPlaceCategory.CAFE));
        queryPort.result = List.of(summary(9L, false));

        service.read(validCommand());

        assertEquals("CAFE", placeCacheRepository.saved.category());
    }

    @Test
    void reloadsExistingPlaceWhenConcurrentSaveCreatesDuplicatePlaceCache() {
        searchPort.result = List.of(searchResult("google-place-id", "니어바이 카페", SoloDiningPlaceCategory.CAFE));
        queryPort.result = List.of(summary(3L, false));
        placeCacheRepository.saveException = new DuplicatePlaceCacheException(new RuntimeException());
        placeCacheRepository.existingAfterConflict = new PlaceCache(
                3L,
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

        service.read(validCommand());

        assertEquals(1, placeCacheRepository.saveAttempts);
        assertEquals(2, placeCacheRepository.findAttempts);
        assertEquals(List.of(3L), queryPort.placeIds);
    }

    @Test
    void rejectsInvalidCommand() {
        assertThrows(InvalidSoloDiningPlacesRequestException.class, () -> service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("91.00000000"),
                new BigDecimal("126.97800000"),
                null
        )));

        assertThrows(InvalidSoloDiningPlacesRequestException.class, () -> service.read(new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                SoloDiningPlaceCategory.OTHER
        )));
    }

    private ReadSoloDiningPlacesCommand validCommand() {
        return new ReadSoloDiningPlacesCommand(
                7L,
                new BigDecimal("37.56650000"),
                new BigDecimal("126.97800000"),
                null
        );
    }

    private SoloDiningPlaceSearchResult searchResult(
            final String googlePlaceId,
            final String name,
            final SoloDiningPlaceCategory category
    ) {
        return new SoloDiningPlaceSearchResult(
                googlePlaceId,
                name,
                "서울특별시 중구 세종대로 110",
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                category,
                new BigDecimal("4.30"),
                22870,
                "places/google-place-id/photos/photo-resource",
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private SoloDiningPlaceSummary summary(final Long placeId, final boolean favorite) {
        return new SoloDiningPlaceSummary(
                placeId,
                "google-place-id",
                "니어바이 카페",
                "places/google-place-id/photos/photo-resource",
                SoloDiningPlaceCategory.CAFE,
                80,
                new BigDecimal("4.30"),
                22870,
                favorite,
                new BigDecimal("37.56612000"),
                new BigDecimal("126.97845000"),
                PlaceBusinessStatus.OPERATIONAL
        );
    }

    private static final class FakeSoloDiningPlaceSearchPort implements SoloDiningPlaceSearchPort {

        private SoloDiningPlaceSearchRequest request;
        private List<SoloDiningPlaceSearchResult> result = List.of();

        @Override
        public List<SoloDiningPlaceSearchResult> search(final SoloDiningPlaceSearchRequest request) {
            this.request = request;
            return result;
        }
    }

    private static final class FakePlaceCacheRepository implements PlaceCacheRepository {

        private final Map<String, PlaceCache> places = new HashMap<>();
        private DuplicatePlaceCacheException saveException;
        private PlaceCache existingAfterConflict;
        private PlaceCache saved;
        private int findAttempts;
        private int saveAttempts;

        @Override
        public PlaceCache save(final PlaceCache model) {
            saveAttempts++;
            if (saveException != null) {
                places.put(existingAfterConflict.googlePlaceId(), existingAfterConflict);
                throw saveException;
            }
            saved = model.id() == null ? withId(model, places.size() + 1L) : model;
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

    private static final class FakeSoloDiningPlaceQueryPort implements SoloDiningPlaceQueryPort {

        private Long userId;
        private List<Long> placeIds;
        private List<SoloDiningPlaceSummary> result = List.of();

        @Override
        public List<SoloDiningPlaceSummary> findAllByPlaceIds(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final List<Long> placeIds
        ) {
            this.userId = userId;
            this.placeIds = placeIds;
            return result;
        }
    }
}
