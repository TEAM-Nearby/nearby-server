// 동행 프로필 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionProfileEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionProfile;
import com.sopt.nearby.domain.companion.repository.CompanionProfileRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionProfileRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionProfile, Long, CompanionProfileEntity, Long>
		implements CompanionProfileRepository {

	public CompanionProfileRepositoryAdapter(final CompanionProfileJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
