// 미팅 체크인 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.MeetingCheckInEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.MeetingCheckInJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.MeetingCheckIn;
import com.sopt.nearby.companion.port.out.MeetingCheckInRepository;
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
