// 동행 미팅 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionMeetingEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeeting;
import com.sopt.nearby.companion.port.out.CompanionMeetingRepository;
import java.time.LocalDateTime;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMeetingRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionMeeting, Long, CompanionMeetingEntity, Long>
		implements CompanionMeetingRepository {

	private final CompanionMeetingJpaRepository jpaRepository;

	public CompanionMeetingRepositoryAdapter(final CompanionMeetingJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public boolean completeIfOngoing(final Long meetingId, final LocalDateTime completedAt) {
		return jpaRepository.completeIfOngoing(meetingId, completedAt) == 1;
	}
}
