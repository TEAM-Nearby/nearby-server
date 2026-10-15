// 혼밥 장소 즐겨찾기 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.place.adapter.out.persistence.repository;

import com.sopt.nearby.place.adapter.out.persistence.entity.SoloDiningFavoriteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SoloDiningFavoriteJpaRepository extends JpaRepository<SoloDiningFavoriteEntity, Long> {

    Optional<SoloDiningFavoriteEntity> findByUserIdAndPlaceId(Long userId, Long placeId);

    void deleteByUserIdAndPlaceId(Long userId, Long placeId);
}
