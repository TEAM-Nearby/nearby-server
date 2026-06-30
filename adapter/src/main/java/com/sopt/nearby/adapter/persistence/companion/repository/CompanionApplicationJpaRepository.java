// 동행 신청 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.companion.repository;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionApplicationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionApplicationJpaRepository extends JpaRepository<CompanionApplicationEntity, Long> {
}
