// 혼밥 맛집 목록 조회 쿼리 어댑터의 거리순 정렬과 즐겨찾기 계산을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCategoryEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningPlaceEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningPlaceQueryJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningMapBounds;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class SoloDiningPlaceQueryAdapterTest {

    private static final BigDecimal CURRENT_LATITUDE = new BigDecimal("37.56650000");
    private static final BigDecimal CURRENT_LONGITUDE = new BigDecimal("126.97800000");

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Autowired
    private SoloDiningFavoriteJpaRepository favoriteJpaRepository;

    @Autowired
    private SoloDiningPlaceQueryJpaRepository queryJpaRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void expandsAndMovesMapBoundsWhileKeepingRegistrationCategoryAndUserDistance() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        BigDecimal userLatitude = new BigDecimal("48.8566");
        BigDecimal userLongitude = new BigDecimal("2.3522");
        PlaceCacheEntity near = registeredPlace("near", "restaurant", "48.8567", "2.3523");
        PlaceCacheEntity far = registeredPlace("far", "cafe", "48.88", "2.38");
        PlaceCacheEntity southWest = registeredPlace("south-west", "restaurant", "48.83", "2.30");
        PlaceCacheEntity northEast = registeredPlace("north-east", "restaurant", "48.89", "2.40");
        registeredPlace("outside-south", "restaurant", "48.82999999", "2.35");
        registeredPlace("outside-north", "restaurant", "48.89000001", "2.35");
        registeredPlace("outside-west", "restaurant", "48.85", "2.29999999");
        registeredPlace("outside-east", "restaurant", "48.85", "2.40000001");
        registeredPlace("cafe", "cafe", "48.86", "2.36");
        placeCacheJpaRepository.saveAndFlush(place("unregistered", "미등록 식당", "restaurant", "48.86", "2.36"));
        entityManager.persist(new PlaceCategoryEntity(far.getId(), "CAFE", 0));
        entityManager.persistAndFlush(new PlaceCategoryEntity(far.getId(), "RESTAURANT", 1));
        favoriteJpaRepository.saveAndFlush(favorite(7L, far.getId(), LocalDateTime.of(2026, 10, 20, 12, 0)));

        assertThat(adapter.findAllNearby(7L, userLatitude, userLongitude,
                SoloDiningPlaceCategory.RESTAURANT, 1000, null, null))
                .extracting(SoloDiningPlaceSummary::placeId).containsExactly(near.getId());

        List<SoloDiningPlaceSummary> expanded = adapter.findAllNearby(7L, userLatitude, userLongitude,
                SoloDiningPlaceCategory.RESTAURANT, 1000, bounds("48.83", "48.89", "2.30", "2.40"), 100);
        assertThat(expanded).extracting(SoloDiningPlaceSummary::placeId)
                .containsExactlyInAnyOrder(near.getId(), far.getId(), southWest.getId(), northEast.getId());
        assertThat(expanded).extracting(SoloDiningPlaceSummary::distanceMeters).isSorted();
        SoloDiningPlaceSummary farSummary = expanded.stream()
                .filter(p -> p.placeId().equals(far.getId())).findFirst().orElseThrow();
        assertThat(farSummary.distanceMeters()).isGreaterThan(1000);
        assertThat(farSummary.isFavorite()).isTrue();
        assertThat(farSummary.categories()).containsExactly(
                SoloDiningPlaceCategory.CAFE, SoloDiningPlaceCategory.RESTAURANT);

        List<SoloDiningPlaceSummary> moved = adapter.findAllNearby(7L, userLatitude, userLongitude,
                SoloDiningPlaceCategory.RESTAURANT, 1000, bounds("48.87", "48.885", "2.37", "2.39"), 100);
        assertThat(moved).extracting(SoloDiningPlaceSummary::placeId).containsExactly(far.getId());
        assertThat(moved.getFirst().distanceMeters()).isEqualTo(farSummary.distanceMeters());
    }

    @Test
    void guestQueriesNeverExposeAnotherUsersFavorites() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity place = registeredPlace("guest-place", "cafe", "37.56652", "126.97802");
        favoriteJpaRepository.saveAndFlush(favorite(7L, place.getId(), LocalDateTime.now()));

        List<SoloDiningPlaceSummary> nearby = adapter.findAllNearby(
                null, CURRENT_LATITUDE, CURRENT_LONGITUDE, null, 1000, null, null);
        List<SoloDiningPlaceSummary> bounded = adapter.findAllNearby(
                null, CURRENT_LATITUDE, CURRENT_LONGITUDE, null, 1000,
                bounds("37.56", "37.57", "126.97", "126.99"), 100);
        List<SoloDiningPlaceSummary> detail = adapter.findAllByPlaceIds(
                null, CURRENT_LATITUDE, CURRENT_LONGITUDE, List.of(place.getId()));

        for (List<SoloDiningPlaceSummary> result : List.of(nearby, bounded, detail)) {
            assertThat(result).extracting(SoloDiningPlaceSummary::placeId).containsExactly(place.getId());
            assertThat(result).noneMatch(SoloDiningPlaceSummary::isFavorite);
        }
        assertThat(adapter.findAllByPlaceIds(7L, CURRENT_LATITUDE, CURRENT_LONGITUDE, List.of(place.getId()))
                .getFirst().isFavorite()).isTrue();
    }

    @Test
    void includesBothSidesOfAntimeridianOnlyWithinMapBounds() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity west = registeredPlace("date-west", "restaurant", "0", "179");
        PlaceCacheEntity east = registeredPlace("date-east", "restaurant", "0", "-179");
        registeredPlace("date-outside", "restaurant", "0", "0");
        registeredPlace("date-north", "restaurant", "2", "179.5");
        placeCacheJpaRepository.saveAndFlush(place("date-unregistered", "미등록 식당", "restaurant", "0", "-179.5"));

        assertThat(adapter.findAllNearby(7L, BigDecimal.ZERO, new BigDecimal("179.9"),
                SoloDiningPlaceCategory.RESTAURANT, 1000, bounds("-1", "1", "179", "-179"), 100))
                .extracting(SoloDiningPlaceSummary::placeId)
                .containsExactly(west.getId(), east.getId());
    }

    @Test
    void filtersByEveryStoredCategoryWithoutDuplicatesAndPreservesOrder() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity dessertCafe = placeCacheJpaRepository.saveAndFlush(place(
                "dessert-cafe", "디저트 카페", "restaurant", "37.56652000", "126.97802000"
        ));
        entityManager.persistAndFlush(new SoloDiningPlaceEntity(dessertCafe.getId()));
        entityManager.persist(new PlaceCategoryEntity(dessertCafe.getId(), "DESSERT", 0));
        entityManager.persistAndFlush(new PlaceCategoryEntity(dessertCafe.getId(), "CAFE", 1));
        favoriteJpaRepository.saveAndFlush(favorite(7L, dessertCafe.getId(), LocalDateTime.of(2026, 10, 15, 12, 0)));

        for (SoloDiningPlaceCategory category : List.of(SoloDiningPlaceCategory.DESSERT, SoloDiningPlaceCategory.CAFE)) {
            List<SoloDiningPlaceSummary> nearby = adapter.findAllNearby(
                    7L, CURRENT_LATITUDE, CURRENT_LONGITUDE, category, 1000, null, null);
            assertThat(nearby).extracting(SoloDiningPlaceSummary::placeId).containsExactly(dessertCafe.getId());
            assertThat(nearby.getFirst().category()).isEqualTo(SoloDiningPlaceCategory.DESSERT);
            assertThat(nearby.getFirst().categories()).containsExactly(
                    SoloDiningPlaceCategory.DESSERT, SoloDiningPlaceCategory.CAFE);
            assertThat(adapter.findAllByUserId(7L, CURRENT_LATITUDE, CURRENT_LONGITUDE,
                    category, SoloDiningFavoriteSort.LATEST))
                    .extracting(SoloDiningFavoriteSummary::categories)
                    .containsExactly(List.of(SoloDiningPlaceCategory.DESSERT, SoloDiningPlaceCategory.CAFE));
        }
        assertThat(adapter.findAllNearby(7L, CURRENT_LATITUDE, CURRENT_LONGITUDE, null, 1000, null, null)).hasSize(1);
        assertThat(adapter.findAllNearby(7L, CURRENT_LATITUDE, CURRENT_LONGITUDE,
                SoloDiningPlaceCategory.RESTAURANT, 1000, null, null)).isEmpty();
        assertThat(adapter.findAllByUserId(8L, CURRENT_LATITUDE, CURRENT_LONGITUDE,
                SoloDiningPlaceCategory.DESSERT, SoloDiningFavoriteSort.LATEST)).isEmpty();
        assertThat(adapter.findAllByPlaceIds(7L, CURRENT_LATITUDE, CURRENT_LONGITUDE, List.of(dessertCafe.getId()))
                .getFirst().categories()).containsExactly(SoloDiningPlaceCategory.DESSERT, SoloDiningPlaceCategory.CAFE);
    }

    @Test
    void findsAllNearbyPlacesSortedByDistanceWithFavoriteFlag() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity nearCafe = placeCacheJpaRepository.saveAndFlush(place(
                "near-cafe",
                "가까운 카페",
                "cafe",
                "37.56652000",
                "126.97802000"
        ));
        PlaceCacheEntity farRestaurant = placeCacheJpaRepository.saveAndFlush(place(
                "far-restaurant",
                "먼 식당",
                "restaurant",
                "37.57000000",
                "126.98200000"
        ));
        PlaceCacheEntity outsideRadius = placeCacheJpaRepository.saveAndFlush(place(
                "outside-radius",
                "반경 밖 장소",
                "pub",
                "37.57650000",
                "126.97801000"
        ));
        for (PlaceCacheEntity place : List.of(nearCafe, farRestaurant, outsideRadius)) {
            entityManager.persist(new SoloDiningPlaceEntity(place.getId()));
        }
        entityManager.flush();
        favoriteJpaRepository.saveAndFlush(new SoloDiningFavoriteEntity(
                null,
                7L,
                farRestaurant.getId(),
                LocalDateTime.of(2026, 7, 7, 12, 0)
        ));

        List<SoloDiningPlaceSummary> result = adapter.findAllNearby(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                null,
                1000,
                null,
                null
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SoloDiningPlaceSummary::placeId)
                .containsExactly(nearCafe.getId(), farRestaurant.getId());
        assertThat(result.get(0).address()).isEqualTo("서울특별시 중구 세종대로 110");
        assertThat(result.get(0).category()).isEqualTo(SoloDiningPlaceCategory.CAFE);
        assertThat(result.get(0).isFavorite()).isFalse();
        assertThat(result.get(1).category()).isEqualTo(SoloDiningPlaceCategory.RESTAURANT);
        assertThat(result.get(1).isFavorite()).isTrue();
        assertThat(result.get(0).distanceMeters()).isLessThan(result.get(1).distanceMeters());
    }

    @Test
    void filtersNearbyPlacesByCategory() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity cafe = placeCacheJpaRepository.saveAndFlush(place(
                "cafe",
                "카페",
                "cafe",
                "37.56652000",
                "126.97802000"
        ));
        PlaceCacheEntity restaurant = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant",
                "식당",
                "restaurant",
                "37.56653000",
                "126.97803000"
        ));
        entityManager.persist(new SoloDiningPlaceEntity(cafe.getId()));
        entityManager.persistAndFlush(new SoloDiningPlaceEntity(restaurant.getId()));

        List<SoloDiningPlaceSummary> result = adapter.findAllNearby(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                SoloDiningPlaceCategory.CAFE,
                1000,
                null,
                null
        );

        assertThat(result).extracting(SoloDiningPlaceSummary::placeId).containsExactly(cafe.getId());
    }

    @ParameterizedTest
    @CsvSource({"-180, 180", "179, -179"})
    void limitsMapResultsAfterFilteringAndSorting(final String west, final String east) {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        registeredPlace("far", "restaurant", "0.5", "179.5");
        PlaceCacheEntity olderTie = registeredPlace("older-tie", "restaurant", "0.2", "179.5");
        PlaceCacheEntity newerTie = registeredPlace("newer-tie", "restaurant", "0.2", "179.5");
        PlaceCacheEntity near = registeredPlace("near", "restaurant", "0.1", "179.5");
        registeredPlace("wrong-category", "cafe", "0", "179.5");
        placeCacheJpaRepository.saveAndFlush(
                place("unregistered", "미등록 식당", "restaurant", "0", "179.5"));

        List<SoloDiningPlaceSummary> result = adapter.findAllNearby(
                7L, BigDecimal.ZERO, new BigDecimal("179.5"), SoloDiningPlaceCategory.RESTAURANT,
                1000, bounds("-1", "1", west, east), 3
        );

        assertThat(result).extracting(SoloDiningPlaceSummary::placeId)
                .containsExactly(near.getId(), newerTie.getId(), olderTie.getId());
        assertThat(result).extracting(SoloDiningPlaceSummary::distanceMeters).isSorted();
    }

    @Test
    void mapsUnknownCategoryToOther() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity unknown = placeCacheJpaRepository.saveAndFlush(place(
                "unknown-place",
                "알 수 없는 장소",
                "unknown",
                "37.56652000",
                "126.97802000"
        ));

        List<SoloDiningPlaceSummary> result = adapter.findAllByPlaceIds(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                List.of(unknown.getId())
        );

        assertThat(result.get(0).category()).isEqualTo(SoloDiningPlaceCategory.OTHER);
    }

    @Test
    void findsUserFavoritesFilteredByCategorySortedByLatest() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity oldCafe = placeCacheJpaRepository.saveAndFlush(place(
                "old-cafe",
                "오래된 카페",
                "cafe",
                "37.56652000",
                "126.97802000"
        ));
        PlaceCacheEntity recentCafe = placeCacheJpaRepository.saveAndFlush(place(
                "recent-cafe",
                "최근 카페",
                "cafe",
                "37.57000000",
                "126.98200000"
        ));
        PlaceCacheEntity restaurant = placeCacheJpaRepository.saveAndFlush(place(
                "restaurant",
                "식당",
                "restaurant",
                "37.56651000",
                "126.97801000"
        ));
        PlaceCacheEntity otherUserCafe = placeCacheJpaRepository.saveAndFlush(place(
                "other-user-cafe",
                "다른 사용자 카페",
                "cafe",
                "37.56651000",
                "126.97801000"
        ));
        SoloDiningFavoriteEntity oldFavorite = favoriteJpaRepository.saveAndFlush(favorite(
                7L,
                oldCafe.getId(),
                LocalDateTime.of(2026, 7, 1, 12, 0)
        ));
        SoloDiningFavoriteEntity recentFavorite = favoriteJpaRepository.saveAndFlush(favorite(
                7L,
                recentCafe.getId(),
                LocalDateTime.of(2026, 7, 2, 12, 0)
        ));
        favoriteJpaRepository.saveAndFlush(favorite(
                7L,
                restaurant.getId(),
                LocalDateTime.of(2026, 7, 3, 12, 0)
        ));
        favoriteJpaRepository.saveAndFlush(favorite(
                8L,
                otherUserCafe.getId(),
                LocalDateTime.of(2026, 7, 4, 12, 0)
        ));

        List<SoloDiningFavoriteSummary> result = adapter.findAllByUserId(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                SoloDiningPlaceCategory.CAFE,
                SoloDiningFavoriteSort.LATEST
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SoloDiningFavoriteSummary::favoriteId)
                .containsExactly(recentFavorite.getId(), oldFavorite.getId());
        assertThat(result).extracting(SoloDiningFavoriteSummary::placeId)
                .containsExactly(recentCafe.getId(), oldCafe.getId());
        assertThat(result.get(0).address()).isEqualTo("서울특별시 중구 세종대로 110");
        assertThat(result).allMatch(SoloDiningFavoriteSummary::isFavorite);
        assertThat(result).extracting(SoloDiningFavoriteSummary::category)
                .containsExactly(SoloDiningPlaceCategory.CAFE, SoloDiningPlaceCategory.CAFE);
        assertThat(result.get(0).distanceMeters()).isGreaterThan(result.get(1).distanceMeters());
    }

    @Test
    void findsUserFavoritesSortedByOldestWithNullableCategory() {
        SoloDiningPlaceQueryAdapter adapter = new SoloDiningPlaceQueryAdapter(queryJpaRepository);
        PlaceCacheEntity uncategorized = placeCacheJpaRepository.saveAndFlush(place(
                "uncategorized",
                "카테고리 없음",
                null,
                "37.56652000",
                "126.97802000",
                PlaceBusinessStatus.UNKNOWN
        ));
        PlaceCacheEntity cafe = placeCacheJpaRepository.saveAndFlush(place(
                "cafe",
                "카페",
                "cafe",
                "37.57000000",
                "126.98200000"
        ));
        SoloDiningFavoriteEntity first = favoriteJpaRepository.saveAndFlush(favorite(
                7L,
                uncategorized.getId(),
                LocalDateTime.of(2026, 7, 1, 12, 0)
        ));
        SoloDiningFavoriteEntity second = favoriteJpaRepository.saveAndFlush(favorite(
                7L,
                cafe.getId(),
                LocalDateTime.of(2026, 7, 2, 12, 0)
        ));

        List<SoloDiningFavoriteSummary> result = adapter.findAllByUserId(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                null,
                SoloDiningFavoriteSort.OLDEST
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SoloDiningFavoriteSummary::favoriteId)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.get(0).category()).isNull();
        assertThat(result.get(0).businessStatus()).isEqualTo(PlaceBusinessStatus.UNKNOWN);
        assertThat(result.get(0).photoReference()).isEqualTo("places/uncategorized/photos/photo-resource");
    }

    private SoloDiningMapBounds bounds(
            final String south, final String north, final String west, final String east
    ) {
        return new SoloDiningMapBounds(
                new BigDecimal(south), new BigDecimal(north), new BigDecimal(west), new BigDecimal(east)
        );
    }

    private PlaceCacheEntity registeredPlace(
            final String googlePlaceId, final String category, final String latitude, final String longitude
    ) {
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(
                place(googlePlaceId, googlePlaceId, category, latitude, longitude));
        entityManager.persistAndFlush(new SoloDiningPlaceEntity(place.getId()));
        return place;
    }

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String category,
            final String latitude,
            final String longitude
    ) {
        return place(googlePlaceId, name, category, latitude, longitude, PlaceBusinessStatus.OPERATIONAL);
    }

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String category,
            final String latitude,
            final String longitude,
            final PlaceBusinessStatus businessStatus
    ) {
        return new PlaceCacheEntity(
                null,
                googlePlaceId,
                name,
                "서울특별시 중구 세종대로 110",
                new BigDecimal(latitude),
                new BigDecimal(longitude),
                category,
                null,
                new BigDecimal("4.30"),
                100,
                "places/" + googlePlaceId + "/photos/photo-resource",
                businessStatus
        );
    }

    private SoloDiningFavoriteEntity favorite(
            final Long userId,
            final Long placeId,
            final LocalDateTime createdAt
    ) {
        return new SoloDiningFavoriteEntity(null, userId, placeId, createdAt);
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {
            PlaceCacheEntity.class,
            SoloDiningFavoriteEntity.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            PlaceCacheJpaRepository.class,
            SoloDiningFavoriteJpaRepository.class,
            SoloDiningPlaceQueryJpaRepository.class
    })
    static class TestApplication {
    }
}
