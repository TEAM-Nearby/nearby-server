// 동행 신청 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionApplicationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionApplicationJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.match.CompanionApplication;
import com.sopt.nearby.companion.port.out.CompanionApplicationRepository;
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
