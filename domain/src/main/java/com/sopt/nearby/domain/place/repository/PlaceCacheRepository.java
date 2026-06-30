// 장소 캐시 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.domain.place.repository;

import com.sopt.nearby.domain.common.repository.DomainRepository;
import com.sopt.nearby.domain.place.model.PlaceCache;

public interface PlaceCacheRepository extends DomainRepository<PlaceCache, Long> {
}
