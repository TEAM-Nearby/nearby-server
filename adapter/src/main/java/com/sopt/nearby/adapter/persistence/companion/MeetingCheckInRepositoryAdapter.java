// 미팅 체크인 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.MeetingCheckInEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.MeetingCheckIn;
import com.sopt.nearby.domain.companion.repository.MeetingCheckInRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class MeetingCheckInRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<MeetingCheckIn, Long, MeetingCheckInEntity, Long>
		implements MeetingCheckInRepository {

	public MeetingCheckInRepositoryAdapter(final MeetingCheckInJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
