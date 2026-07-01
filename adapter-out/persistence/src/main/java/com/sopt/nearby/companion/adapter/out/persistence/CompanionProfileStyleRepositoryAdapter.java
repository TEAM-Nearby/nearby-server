// 동행 프로필 성향 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileStyleJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfileStyle;
import com.sopt.nearby.companion.port.out.CompanionProfileStyleRepository;
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
