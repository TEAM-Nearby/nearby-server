// 동행 모집글 성향 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostStyleEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostStyleJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStyle;
import com.sopt.nearby.companion.port.out.CompanionPostStyleRepository;
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
