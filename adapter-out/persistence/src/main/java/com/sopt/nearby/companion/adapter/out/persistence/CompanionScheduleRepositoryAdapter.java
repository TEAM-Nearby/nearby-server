// 동행 일정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionScheduleEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.meeting.CompanionSchedule;
import com.sopt.nearby.companion.port.out.CompanionScheduleRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionScheduleRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionSchedule, Long, CompanionScheduleEntity, Long>
		implements CompanionScheduleRepository {

	public CompanionScheduleRepositoryAdapter(final CompanionScheduleJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
