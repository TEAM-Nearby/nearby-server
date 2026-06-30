// 동행 모집글 성향 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.companion.repository;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostStyleEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostStyleEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionPostStyleJpaRepository
		extends JpaRepository<CompanionPostStyleEntity, CompanionPostStyleEntityId> {
}
