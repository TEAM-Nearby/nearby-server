// 혼밥 맛집 목록 조회 쿼리 어댑터의 거리순 정렬과 즐겨찾기 계산을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningPlaceQueryJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

    @Test
    void findsCandidatePlacesSortedByDistanceWithFavoriteFlag() {
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
        placeCacheJpaRepository.saveAndFlush(place(
                "not-candidate",
                "후보가 아닌 장소",
                "pub",
                "37.56651000",
                "126.97801000"
        ));
        favoriteJpaRepository.saveAndFlush(new SoloDiningFavoriteEntity(
                null,
                7L,
                farRestaurant.getId(),
                LocalDateTime.of(2026, 7, 7, 12, 0)
        ));

        List<SoloDiningPlaceSummary> result = adapter.findAllByPlaceIds(
                7L,
                CURRENT_LATITUDE,
                CURRENT_LONGITUDE,
                List.of(farRestaurant.getId(), nearCafe.getId())
        );

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SoloDiningPlaceSummary::placeId)
                .containsExactly(nearCafe.getId(), farRestaurant.getId());
        assertThat(result.get(0).category()).isEqualTo(SoloDiningPlaceCategory.CAFE);
        assertThat(result.get(0).isFavorite()).isFalse();
        assertThat(result.get(1).category()).isEqualTo(SoloDiningPlaceCategory.RESTAURANT);
        assertThat(result.get(1).isFavorite()).isTrue();
        assertThat(result.get(0).distanceMeters()).isLessThan(result.get(1).distanceMeters());
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
