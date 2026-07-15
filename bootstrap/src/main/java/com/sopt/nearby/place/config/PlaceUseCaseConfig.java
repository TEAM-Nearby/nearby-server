// 장소 유스케이스 구현체를 Spring Bean으로 조립하는 설정 클래스다.
package com.sopt.nearby.place.config;

import com.sopt.nearby.place.application.ReadSoloDiningPlaceService;
import com.sopt.nearby.place.application.ReadSoloDiningFavoritesService;
import com.sopt.nearby.place.application.ReadSoloDiningPlacesService;
import com.sopt.nearby.place.application.ManageSoloDiningFavoriteService;
import com.sopt.nearby.place.application.ResolvePlaceCacheService;
import com.sopt.nearby.place.application.ResolvePlaceImageService;
import com.sopt.nearby.place.port.in.ManageSoloDiningFavoriteUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.in.ResolvePlaceImageUseCase;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
import com.sopt.nearby.place.port.out.PlaceImageLookupPort;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteQueryPort;
import com.sopt.nearby.place.port.out.SoloDiningFavoriteRepository;
import com.sopt.nearby.place.port.out.SoloDiningPlaceDetailsPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceQueryPort;
import com.sopt.nearby.place.port.out.SoloDiningPlaceSearchPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaceUseCaseConfig {

    @Bean
    ResolvePlaceCacheUseCase resolvePlaceCacheUseCase(
            final PlaceCacheRepository placeCacheRepository
    ) {
        return new ResolvePlaceCacheService(placeCacheRepository);
    }

    @Bean
    ResolvePlaceImageUseCase resolvePlaceImageUseCase(
            final PlaceImageLookupPort placeImageLookupPort,
            @Value("${nearby.place.default-image-url}") final String defaultPlaceImageUrl
    ) {
        return new ResolvePlaceImageService(placeImageLookupPort, defaultPlaceImageUrl);
    }

    @Bean
    ReadSoloDiningPlacesUseCase readSoloDiningPlacesUseCase(
            final SoloDiningPlaceSearchPort soloDiningPlaceSearchPort,
            final PlaceCacheRepository placeCacheRepository,
            final SoloDiningPlaceQueryPort soloDiningPlaceQueryPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        return new ReadSoloDiningPlacesService(
                soloDiningPlaceSearchPort,
                placeCacheRepository,
                soloDiningPlaceQueryPort,
                resolvePlaceImageUseCase
        );
    }

    @Bean
    ReadSoloDiningPlaceUseCase readSoloDiningPlaceUseCase(
            final SoloDiningPlaceQueryPort soloDiningPlaceQueryPort,
            final SoloDiningPlaceDetailsPort soloDiningPlaceDetailsPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        return new ReadSoloDiningPlaceService(
                soloDiningPlaceQueryPort,
                soloDiningPlaceDetailsPort,
                resolvePlaceImageUseCase
        );
    }

    @Bean
    ReadSoloDiningFavoritesUseCase readSoloDiningFavoritesUseCase(
            final SoloDiningFavoriteQueryPort soloDiningFavoriteQueryPort,
            final ResolvePlaceImageUseCase resolvePlaceImageUseCase
    ) {
        return new ReadSoloDiningFavoritesService(soloDiningFavoriteQueryPort, resolvePlaceImageUseCase);
    }

    @Bean
    ManageSoloDiningFavoriteUseCase manageSoloDiningFavoriteUseCase(
            final PlaceCacheRepository placeCacheRepository,
            final SoloDiningFavoriteRepository soloDiningFavoriteRepository
    ) {
        return new ManageSoloDiningFavoriteService(placeCacheRepository, soloDiningFavoriteRepository);
    }
}
