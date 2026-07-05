// 장소 캐시 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.place.adapter.out.persistence.repository;

import com.sopt.nearby.place.adapter.out.persistence.entity.PlaceCacheEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceCacheJpaRepository extends JpaRepository<PlaceCacheEntity, Long> {
    Optional<PlaceCacheEntity> findByGooglePlaceId(String googlePlaceId);
}
