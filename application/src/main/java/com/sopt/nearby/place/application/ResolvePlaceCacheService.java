// 장소 캐시를 조회하거나 없으면 저장하는 유스케이스 구현체다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.DuplicatePlaceCacheException;
import com.sopt.nearby.place.domain.model.PlaceBusinessStatus;
import com.sopt.nearby.place.domain.model.PlaceCache;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheCommand;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvedPlaceCache;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;

public class ResolvePlaceCacheService implements ResolvePlaceCacheUseCase {

    private final PlaceCacheRepository placeCacheRepository;

    public ResolvePlaceCacheService(final PlaceCacheRepository placeCacheRepository) {
        this.placeCacheRepository = placeCacheRepository;
    }

    @Override
    public ResolvedPlaceCache resolve(final ResolvePlaceCacheCommand command) {
        return placeCacheRepository.findByGooglePlaceId(command.googlePlaceId())
                .map(place -> updateIfCategoryExplicit(place, command))
                .map(this::toResolvedPlaceCache)
                .orElseGet(() -> saveOrReload(command));
    }

    private PlaceCache updateIfCategoryExplicit(final PlaceCache place, final ResolvePlaceCacheCommand command) {
        if (command.category() == null || command.category().isBlank()) {
            return place;
        }
        return placeCacheRepository.save(new PlaceCache(
                place.id(),
                place.googlePlaceId(),
                command.name(),
                command.address(),
                command.latitude(),
                command.longitude(),
                command.category(),
                place.phoneNumber(),
                place.rating(),
                place.reviewCount(),
                place.photoReference(),
                place.businessStatus()
        ));
    }

    private ResolvedPlaceCache saveOrReload(final ResolvePlaceCacheCommand command) {
        try {
            return toResolvedPlaceCache(placeCacheRepository.save(newPlaceCache(command)));
        } catch (DuplicatePlaceCacheException exception) {
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
                command.category() == null || command.category().isBlank() ? "OTHER" : command.category(),
                null,
                null,
                null,
                null,
                PlaceBusinessStatus.UNKNOWN
        );
    }

    private ResolvedPlaceCache toResolvedPlaceCache(final PlaceCache placeCache) {
        return new ResolvedPlaceCache(
                placeCache.id(),
                placeCache.googlePlaceId(),
                placeCache.name(),
                placeCache.address(),
                placeCache.latitude(),
                placeCache.longitude(),
                placeCache.category()
        );
    }
}
