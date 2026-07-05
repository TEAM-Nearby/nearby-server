// 장소 유스케이스 구현체를 Spring Bean으로 조립하는 설정 클래스다.
package com.sopt.nearby.place.config;

import com.sopt.nearby.place.application.ResolvePlaceCacheService;
import com.sopt.nearby.place.port.in.ResolvePlaceCacheUseCase;
import com.sopt.nearby.place.port.out.PlaceCacheRepository;
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
}
