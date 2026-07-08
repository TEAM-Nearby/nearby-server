// 혼밥 맛집 목록 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.place.adapter.out.persistence;

import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningFavoriteProjection;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningPlaceProjection;
import com.sopt.nearby.place.adapter.out.persistence.repository.SoloDiningPlaceQueryJpaRepository;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSort;
import com.sopt.nearby.place.domain.model.SoloDiningFavoriteSummary;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceSummary;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Repository;

@Repository
public class SoloDiningPlaceQueryAdapter implements SoloDiningPlaceQueryPort, SoloDiningFavoriteQueryPort {

    private final SoloDiningPlaceQueryJpaRepository repository;

    public SoloDiningPlaceQueryAdapter(final SoloDiningPlaceQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SoloDiningPlaceSummary> findAllByPlaceIds(
            final Long userId,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final List<Long> placeIds
    ) {
        return repository.findAllByPlaceIds(userId, latitude, longitude, placeIds)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<SoloDiningFavoriteSummary> findAllByUserId(
            final Long userId,
            final BigDecimal latitude,
            final BigDecimal longitude,
            final SoloDiningPlaceCategory category,
            final SoloDiningFavoriteSort sort
    ) {
        return repository.findAllFavoritesByUserId(
                        userId,
                        latitude,
                        longitude,
                        category == null ? null : category.name(),
                        sort.name()
                )
                .stream()
                .map(this::toFavoriteSummary)
                .toList();
    }

    private SoloDiningPlaceSummary toSummary(final SoloDiningPlaceProjection row) {
        return new SoloDiningPlaceSummary(
                row.getPlaceId(),
                row.getGooglePlaceId(),
                row.getName(),
                row.getPhotoReference(),
                category(row.getCategory()),
                row.getDistanceMeters().intValue(),
                row.getRating(),
                row.getReviewCount(),
                Boolean.TRUE.equals(row.getFavorite()),
                row.getLatitude(),
                row.getLongitude(),
                businessStatus(row.getBusinessStatus())
        );
    }

    private SoloDiningFavoriteSummary toFavoriteSummary(final SoloDiningFavoriteProjection row) {
        return new SoloDiningFavoriteSummary(
                row.getFavoriteId(),
                row.getCreatedAt(),
                row.getPlaceId(),
                row.getGooglePlaceId(),
                row.getName(),
                row.getPhotoReference(),
                nullableCategory(row.getCategory()),
                row.getDistanceMeters().intValue(),
                row.getRating(),
                row.getReviewCount(),
                true,
                businessStatus(row.getBusinessStatus())
        );
    }

    private SoloDiningPlaceCategory category(final String value) {
        try {
            return SoloDiningPlaceCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return SoloDiningPlaceCategory.OTHER;
        }
    }

    private SoloDiningPlaceCategory nullableCategory(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return category(value);
    }

    private PlaceBusinessStatus businessStatus(final String value) {
        try {
            return PlaceBusinessStatus.valueOf(value);
        } catch (RuntimeException exception) {
            return PlaceBusinessStatus.UNKNOWN;
        }
    }
}
