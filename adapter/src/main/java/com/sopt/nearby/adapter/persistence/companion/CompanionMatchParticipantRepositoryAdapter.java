// 동행 매칭 참여자 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionMatchParticipant;
import com.sopt.nearby.domain.companion.repository.CompanionMatchParticipantRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMatchParticipantRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionMatchParticipant, Long, CompanionMatchParticipantEntity, Long>
		implements CompanionMatchParticipantRepository {

	public CompanionMatchParticipantRepositoryAdapter(final CompanionMatchParticipantJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
