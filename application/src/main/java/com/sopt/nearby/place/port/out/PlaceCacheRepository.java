// 장소 캐시 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.common.port.DomainRepository;
import com.sopt.nearby.place.domain.model.PlaceCache;
import java.util.Optional;

public interface PlaceCacheRepository extends DomainRepository<PlaceCache, Long> {
    Optional<PlaceCache> findByGooglePlaceId(String googlePlaceId);
}
