// 미팅 취소 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCancellationEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCancellationJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.meeting.MeetingCancellation;
import com.sopt.nearby.companion.port.out.MeetingCancellationRepository;
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
