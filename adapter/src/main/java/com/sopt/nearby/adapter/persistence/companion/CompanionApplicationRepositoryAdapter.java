// 동행 신청 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionApplicationEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionApplication;
import com.sopt.nearby.domain.companion.repository.CompanionApplicationRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionApplicationRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionApplication, Long, CompanionApplicationEntity, Long>
		implements CompanionApplicationRepository {

	public CompanionApplicationRepositoryAdapter(final CompanionApplicationJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
