// 동행 프로필 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionProfileJpaRepository extends JpaRepository<CompanionProfileEntity, Long> {
    List<CompanionProfileEntity> findAllByUserIdIn(List<Long> userIds);
}
