// 혼밥 장소 즐겨찾기 저장소 어댑터의 중복 저장 예외 변환을 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteJpaRepository;
import com.sopt.nearby.place.domain.exception.DuplicateSoloDiningFavoriteException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavorite;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class SoloDiningFavoriteRepositoryAdapterTest {

    @Autowired
    private SoloDiningFavoriteJpaRepository favoriteJpaRepository;

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void translatesDuplicateUserPlaceViolation() {
        SoloDiningFavoriteRepositoryAdapter adapter = new SoloDiningFavoriteRepositoryAdapter(favoriteJpaRepository);
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(place());
        favoriteJpaRepository.saveAndFlush(new SoloDiningFavoriteEntity(
                null,
                7L,
                place.getId(),
                LocalDateTime.of(2026, 7, 3, 13, 20)
        ));

        assertThatThrownBy(() -> adapter.save(new SoloDiningFavorite(
                null,
                7L,
                place.getId(),
                LocalDateTime.of(2026, 7, 3, 13, 21)
        ))).isInstanceOf(DuplicateSoloDiningFavoriteException.class);
    }

    private PlaceCacheEntity place() {
        return new PlaceCacheEntity(
                null,
                "google-place-id",
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = SoloDiningFavoriteEntity.class)
    @EnableJpaRepositories(basePackageClasses = SoloDiningFavoriteJpaRepository.class)
    static class TestApplication {
    }
}
