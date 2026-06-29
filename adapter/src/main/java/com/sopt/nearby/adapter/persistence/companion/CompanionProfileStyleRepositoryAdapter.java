// 동행 프로필 성향 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileStyleEntityId;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionProfileStyleJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionProfileStyle;
import com.sopt.nearby.domain.companion.repository.CompanionProfileStyleRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionProfileStyleRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionProfileStyle, CompanionProfileStyle.Key,
				CompanionProfileStyleEntity, CompanionProfileStyleEntityId>
		implements CompanionProfileStyleRepository {

	public CompanionProfileStyleRepositoryAdapter(final CompanionProfileStyleJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				CompanionPersistenceMapper::toEntityId);
	}
}
