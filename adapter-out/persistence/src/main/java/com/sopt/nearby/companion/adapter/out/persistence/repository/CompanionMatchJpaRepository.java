// 동행 매칭 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionMatchJpaRepository extends JpaRepository<CompanionMatchEntity, Long> {
}
