// 혼밥 맛집 목록 조회 유스케이스를 구현한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.domain.model.SoloDiningPlaceCategory;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchRequest;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchResult;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

public class ReadSoloDiningPlacesService implements ReadSoloDiningPlacesUseCase {

    private static final int SEARCH_RADIUS_METERS = 1000;
    private static final int MAX_RESULT_COUNT = 20;
    private static final BigDecimal MIN_LATITUDE = new BigDecimal("-90");
    private static final BigDecimal MAX_LATITUDE = new BigDecimal("90");
    private static final BigDecimal MIN_LONGITUDE = new BigDecimal("-180");
    private static final BigDecimal MAX_LONGITUDE = new BigDecimal("180");
    private static final List<String> ALL_GOOGLE_TYPES = List.of("restaurant", "cafe", "pub");

    private final SoloDiningPlaceSearchPort searchPort;
    private final PlaceCacheRepository placeCacheRepository;
    private final SoloDiningPlaceQueryPort queryPort;

    public ReadSoloDiningPlacesService(
            final SoloDiningPlaceSearchPort searchPort,
            final PlaceCacheRepository placeCacheRepository,
            final SoloDiningPlaceQueryPort queryPort
    ) {
        this.searchPort = searchPort;
        this.placeCacheRepository = placeCacheRepository;
        this.queryPort = queryPort;
    }

    @Override
    @Transactional
    public SoloDiningPlacesResult read(final ReadSoloDiningPlacesCommand command) {
        validate(command);

        List<Long> placeIds = searchPort.search(new SoloDiningPlaceSearchRequest(
                        command.latitude(),
                        command.longitude(),
                        SEARCH_RADIUS_METERS,
                        MAX_RESULT_COUNT,
                        includedTypes(command.category())
                ))
                .stream()
                .filter(this::cacheable)
                .map(this::saveOrReload)
                .map(PlaceCache::id)
                .toList();

        if (placeIds.isEmpty()) {
            return new SoloDiningPlacesResult(List.of());
        }

        return new SoloDiningPlacesResult(queryPort.findAllByPlaceIds(
                command.userId(),
                command.latitude(),
                command.longitude(),
                placeIds
        ));
    }

    private PlaceCache saveOrReload(final SoloDiningPlaceSearchResult result) {
        return placeCacheRepository.findByGooglePlaceId(result.googlePlaceId())
                .map(existing -> placeCacheRepository.save(toPlaceCache(
                        existing.id(),
                        existing.category(),
                        existing.phoneNumber(),
                        result
                )))
                .orElseGet(() -> saveNewOrReload(result));
    }

    private PlaceCache saveNewOrReload(final SoloDiningPlaceSearchResult result) {
        try {
            return placeCacheRepository.save(toPlaceCache(null, null, null, result));
        } catch (DataIntegrityViolationException exception) {
            return placeCacheRepository.findByGooglePlaceId(result.googlePlaceId())
                    .orElseThrow(() -> exception);
        }
    }

    private PlaceCache toPlaceCache(
            final Long id,
            final String existingCategory,
            final String phoneNumber,
            final SoloDiningPlaceSearchResult result
    ) {
        return new PlaceCache(
                id,
                result.googlePlaceId(),
                result.name(),
                result.address(),
                result.latitude(),
                result.longitude(),
                category(existingCategory, result.category()),
                phoneNumber,
                result.rating(),
                result.reviewCount(),
                result.photoReference(),
                result.businessStatus() == null ? PlaceBusinessStatus.UNKNOWN : result.businessStatus()
        );
    }

    private String category(final String existingCategory, final SoloDiningPlaceCategory googleCategory) {
        if (!isBlank(existingCategory) && !"OTHER".equalsIgnoreCase(existingCategory)) {
            return existingCategory;
        }
        return category(googleCategory).name();
    }

    private SoloDiningPlaceCategory category(final SoloDiningPlaceCategory category) {
        return category == null ? SoloDiningPlaceCategory.OTHER : category;
    }

    private List<String> includedTypes(final SoloDiningPlaceCategory category) {
        if (category == null) {
            return ALL_GOOGLE_TYPES;
        }
        return switch (category) {
            case RESTAURANT -> List.of("restaurant");
            case CAFE -> List.of("cafe");
            case PUB -> List.of("pub");
            case OTHER -> throw new InvalidSoloDiningPlacesRequestException();
        };
    }

    private boolean cacheable(final SoloDiningPlaceSearchResult result) {
        return result != null
                && !isBlank(result.googlePlaceId())
                && !isBlank(result.name())
                && result.latitude() != null
                && result.longitude() != null;
    }

    private void validate(final ReadSoloDiningPlacesCommand command) {
        if (command == null
                || command.userId() == null
                || command.latitude() == null
                || command.longitude() == null
                || command.category() == SoloDiningPlaceCategory.OTHER
                || command.latitude().compareTo(MIN_LATITUDE) < 0
                || command.latitude().compareTo(MAX_LATITUDE) > 0
                || command.longitude().compareTo(MIN_LONGITUDE) < 0
                || command.longitude().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidSoloDiningPlacesRequestException();
        }
    }

    private boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }
}
