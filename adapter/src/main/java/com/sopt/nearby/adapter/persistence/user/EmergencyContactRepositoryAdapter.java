// 긴급 연락처 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.user;

import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.adapter.persistence.user.entity.EmergencyContactEntity;
import com.sopt.nearby.adapter.persistence.user.mapper.UserPersistenceMapper;
import com.sopt.nearby.adapter.persistence.user.repository.EmergencyContactJpaRepository;
import com.sopt.nearby.domain.user.model.EmergencyContact;
import com.sopt.nearby.domain.user.repository.EmergencyContactRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class EmergencyContactRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<EmergencyContact, Long, EmergencyContactEntity, Long>
		implements EmergencyContactRepository {

	public EmergencyContactRepositoryAdapter(final EmergencyContactJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
	}
}
