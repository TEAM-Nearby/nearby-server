// 장소 유스케이스 설정이 상세 조회 유스케이스 빈을 조립하는지 검증한다.
package com.sopt.nearby.place.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceImage;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsResult;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceUseCaseConfigTest {

    @Test
    void createsReadSoloDiningPlaceUseCase() {
        ReadSoloDiningPlaceUseCase useCase = new PlaceUseCaseConfig()
                .readSoloDiningPlaceUseCase(
                        new FakeSoloDiningPlaceQueryPort(),
                        new FakeSoloDiningPlaceDetailsPort(),
                        command -> new ResolvedPlaceImage(
                                "https://nearby.sopt.org/images/default-place.png",
                                ResolvedPlaceImage.DEFAULT,
                                List.of()
                        )
                );

        assertNotNull(useCase);
    }

    @Test
    void createsReadSoloDiningFavoritesUseCase() {
        ReadSoloDiningFavoritesUseCase useCase = new PlaceUseCaseConfig()
                .readSoloDiningFavoritesUseCase(
                        new FakeSoloDiningFavoriteQueryPort(),
                        new FakeResolvePlaceImageUseCase()
                );

        assertNotNull(useCase);
    }

    private static final class FakeSoloDiningPlaceQueryPort implements SoloDiningPlaceQueryPort {

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
            return List.of();
        }
    }

    private static final class FakeSoloDiningPlaceDetailsPort implements SoloDiningPlaceDetailsPort {

        @Override
        public SoloDiningPlaceDetailsResult findByGooglePlaceId(final String googlePlaceId) {
            return null;
        }
    }

    private static final class FakeSoloDiningFavoriteQueryPort implements SoloDiningFavoriteQueryPort {

        @Override
        public List<SoloDiningFavoriteSummary> findAllByUserId(
                final Long userId,
                final BigDecimal latitude,
                final BigDecimal longitude,
                final SoloDiningPlaceCategory category,
                final SoloDiningFavoriteSort sort
        ) {
            return List.of();
        }
    }

    private static final class FakeResolvePlaceImageUseCase implements ResolvePlaceImageUseCase {

        @Override
        public ResolvedPlaceImage resolve(final ResolvePlaceImageCommand command) {
            return new ResolvedPlaceImage(
                    "https://nearby.sopt.org/images/default-place.png",
                    ResolvedPlaceImage.DEFAULT,
                    List.of()
            );
        }
    }
}
