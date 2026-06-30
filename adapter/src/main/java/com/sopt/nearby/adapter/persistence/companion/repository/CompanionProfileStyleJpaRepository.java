// 동행 프로필 성향 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.adapter.persistence.companion.repository;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileStyleEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionProfileStyleJpaRepository
		extends JpaRepository<CompanionProfileStyleEntity, CompanionProfileStyleEntityId> {
}
