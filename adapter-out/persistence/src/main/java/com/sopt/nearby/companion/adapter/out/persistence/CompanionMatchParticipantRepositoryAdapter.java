// 동행 매칭 참여자 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMatchParticipantEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchParticipantJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchParticipant;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
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
