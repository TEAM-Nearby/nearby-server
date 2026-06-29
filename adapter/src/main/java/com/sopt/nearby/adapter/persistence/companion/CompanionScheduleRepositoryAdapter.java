// 동행 일정 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionScheduleEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionScheduleJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionSchedule;
import com.sopt.nearby.domain.companion.repository.CompanionScheduleRepository;
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
