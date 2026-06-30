// 동행 매칭 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMatchEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionMatch;
import com.sopt.nearby.domain.companion.repository.CompanionMatchRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMatchRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionMatch, Long, CompanionMatchEntity, Long>
		implements CompanionMatchRepository {

	public CompanionMatchRepositoryAdapter(final CompanionMatchJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
