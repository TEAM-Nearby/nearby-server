// 동행 일정 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionScheduleJpaRepository extends JpaRepository<CompanionScheduleEntity, Long> {
    Optional<CompanionScheduleEntity> findByMatchIdAndConfirmedTrue(Long matchId);
}
