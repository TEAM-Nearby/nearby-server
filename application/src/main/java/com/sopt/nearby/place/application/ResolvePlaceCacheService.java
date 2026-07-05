// 장소 캐시를 조회하거나 없으면 저장하는 유스케이스 구현체다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import org.springframework.dao.DataIntegrityViolationException;

public class ResolvePlaceCacheService implements ResolvePlaceCacheUseCase {

    private final PlaceCacheRepository placeCacheRepository;

    public ResolvePlaceCacheService(final PlaceCacheRepository placeCacheRepository) {
        this.placeCacheRepository = placeCacheRepository;
    }

    @Override
    public ResolvedPlaceCache resolve(final ResolvePlaceCacheCommand command) {
        return placeCacheRepository.findByGooglePlaceId(command.googlePlaceId())
                .map(this::toResolvedPlaceCache)
                .orElseGet(() -> saveOrReload(command));
    }

    private ResolvedPlaceCache saveOrReload(final ResolvePlaceCacheCommand command) {
        try {
            return toResolvedPlaceCache(placeCacheRepository.save(newPlaceCache(command)));
        } catch (DataIntegrityViolationException exception) {
            return placeCacheRepository.findByGooglePlaceId(command.googlePlaceId())
                    .map(this::toResolvedPlaceCache)
                    .orElseThrow(() -> exception);
        }
    }

    private PlaceCache newPlaceCache(final ResolvePlaceCacheCommand command) {
        return new PlaceCache(
                null,
                command.googlePlaceId(),
                command.name(),
                command.address(),
                command.latitude(),
                command.longitude(),
                null,
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        );
    }

    private ResolvedPlaceCache toResolvedPlaceCache(final PlaceCache placeCache) {
        return new ResolvedPlaceCache(placeCache.id());
    }
}
