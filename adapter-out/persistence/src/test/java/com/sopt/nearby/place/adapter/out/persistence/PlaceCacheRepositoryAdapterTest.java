// 장소 캐시 저장소 어댑터의 Google Place ID 조회를 검증한다.
package com.sopt.nearby.place.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import com.sopt.nearby.place.adapter.out.persistence.repository.PlaceCacheJpaRepository;
import com.sopt.nearby.place.domain.exception.DuplicatePlaceCacheException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@DataJpaTest
class PlaceCacheRepositoryAdapterTest {

    @Autowired
    private PlaceCacheJpaRepository placeCacheJpaRepository;

    @Test
    void findsPlaceCacheByGooglePlaceId() {
        PlaceCacheRepositoryAdapter adapter = new PlaceCacheRepositoryAdapter(placeCacheJpaRepository);
        PlaceCacheEntity place = placeCacheJpaRepository.saveAndFlush(new PlaceCacheEntity(
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
        ));

        Optional<PlaceCache> result = adapter.findByGooglePlaceId("google-place-id");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(place.getId());
        assertThat(result.get().name()).isEqualTo("Siutat condal");
        assertThat(adapter.findByGooglePlaceId("missing-place-id")).isEmpty();
    }

    @Test
    void translatesDuplicateGooglePlaceIdViolation() {
        PlaceCacheRepositoryAdapter adapter = new PlaceCacheRepositoryAdapter(placeCacheJpaRepository);
        placeCacheJpaRepository.saveAndFlush(entity("google-place-id"));

        assertThatThrownBy(() -> adapter.save(placeCache("google-place-id")))
                .isInstanceOf(DuplicatePlaceCacheException.class);
    }

    private PlaceCacheEntity entity(final String googlePlaceId) {
        return new PlaceCacheEntity(
                null,
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

    private PlaceCache placeCache(final String googlePlaceId) {
        return new PlaceCache(
                null,
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

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = PlaceCacheEntity.class)
    @EnableJpaRepositories(basePackageClasses = PlaceCacheJpaRepository.class)
    static class TestApplication {
    }
}
