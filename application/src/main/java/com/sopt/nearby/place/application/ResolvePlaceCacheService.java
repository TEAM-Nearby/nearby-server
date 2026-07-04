// 장소 캐시를 조회하거나 없으면 저장하는 유스케이스 구현체다.
package com.sopt.nearby.place.application;

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
        PlaceCache place = placeCacheRepository.findByGooglePlaceId(command.googlePlaceId())
                .orElseGet(() -> placeCacheRepository.save(new PlaceCache(
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
                )));

        return new ResolvedPlaceCache(place.id());
    }
}
