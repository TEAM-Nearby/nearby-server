// 장소 캐시 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.place.repository;

import com.sopt.nearby.adapter.persistence.place.entity.PlaceCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceCacheJpaRepository extends JpaRepository<PlaceCacheEntity, Long> {
}
