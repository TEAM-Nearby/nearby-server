// 동행 매칭 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.match.CompanionMatch;
import com.sopt.nearby.companion.port.out.CompanionMatchRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMatchRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionMatch, Long, CompanionMatchEntity, Long>
		implements CompanionMatchRepository {

	private final CompanionMatchJpaRepository jpaRepository;

	public CompanionMatchRepositoryAdapter(final CompanionMatchJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public boolean confirmScheduleIfMatched(final Long matchId) {
		return jpaRepository.confirmScheduleIfMatched(matchId) == 1;
	}
}
