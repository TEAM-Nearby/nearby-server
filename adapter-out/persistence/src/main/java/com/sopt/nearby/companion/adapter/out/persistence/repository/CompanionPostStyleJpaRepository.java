// 동행 모집글 성향 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionPostStyleJpaRepository
		extends JpaRepository<CompanionPostStyleEntity, CompanionPostStyleEntityId> {
}
