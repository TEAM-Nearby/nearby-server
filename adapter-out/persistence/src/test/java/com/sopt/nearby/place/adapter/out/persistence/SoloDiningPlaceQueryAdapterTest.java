// 혼밥 맛집 목록 조회 쿼리 어댑터의 거리순 정렬과 즐겨찾기 계산을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningPlaceQueryJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
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
        favoriteJpaRepository.saveAndFlush(new SoloDiningFavoriteEntity(
                null,
                7L,
                farRestaurant.getId(),
                LocalDateTime.of(2026, 7, 7, 12, 1)
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

    private PlaceCacheEntity place(
            final String googlePlaceId,
            final String name,
            final String category,
            final String latitude,
            final String longitude
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
                PlaceBusinessStatus.OPERATIONAL
        );
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
