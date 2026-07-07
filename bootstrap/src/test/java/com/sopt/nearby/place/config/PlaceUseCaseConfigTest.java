// 장소 유스케이스 설정이 상세 조회 유스케이스 빈을 조립하는지 검증한다.
package com.sopt.nearby.place.config;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
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
                .readSoloDiningPlaceUseCase(new FakeSoloDiningPlaceQueryPort(), new FakeSoloDiningPlaceDetailsPort());

        assertNotNull(useCase);
    }

    private static final class FakeSoloDiningPlaceQueryPort implements SoloDiningPlaceQueryPort {

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
}
