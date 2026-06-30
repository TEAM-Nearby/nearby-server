// 미팅 취소 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.MeetingCancellationEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.MeetingCancellationJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.MeetingCancellation;
import com.sopt.nearby.domain.companion.repository.MeetingCancellationRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingCancellationRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<MeetingCancellation, Long, MeetingCancellationEntity, Long>
		implements MeetingCancellationRepository {

	public MeetingCancellationRepositoryAdapter(final MeetingCancellationJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
