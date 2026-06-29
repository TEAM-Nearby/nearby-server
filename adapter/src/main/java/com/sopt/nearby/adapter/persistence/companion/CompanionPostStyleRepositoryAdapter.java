// 동행 모집글 성향 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostStyleEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostStyleEntityId;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionPostStyleJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionPostStyle;
import com.sopt.nearby.domain.companion.repository.CompanionPostStyleRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostStyleRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionPostStyle, CompanionPostStyle.Key,
				CompanionPostStyleEntity, CompanionPostStyleEntityId>
		implements CompanionPostStyleRepository {

	public CompanionPostStyleRepositoryAdapter(final CompanionPostStyleJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				CompanionPersistenceMapper::toEntityId);
	}
}
