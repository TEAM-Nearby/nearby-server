// 동행 미팅 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionMeetingEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionMeeting;
import com.sopt.nearby.domain.companion.repository.CompanionMeetingRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMeetingRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionMeeting, Long, CompanionMeetingEntity, Long>
		implements CompanionMeetingRepository {

	public CompanionMeetingRepositoryAdapter(final CompanionMeetingJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
