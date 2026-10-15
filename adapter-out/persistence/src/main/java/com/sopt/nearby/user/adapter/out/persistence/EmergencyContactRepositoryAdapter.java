// 긴급 연락처 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.user.adapter.out.persistence;

import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.user.adapter.out.persistence.entity.EmergencyContactEntity;
import com.sopt.nearby.user.adapter.out.persistence.mapper.UserPersistenceMapper;
import com.sopt.nearby.user.adapter.out.persistence.repository.EmergencyContactJpaRepository;
import com.sopt.nearby.user.domain.model.EmergencyContact;
import com.sopt.nearby.user.port.out.EmergencyContactRepository;
import java.util.function.Function;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class EmergencyContactRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<EmergencyContact, Long, EmergencyContactEntity, Long>
		implements EmergencyContactRepository {

	private final EmergencyContactJpaRepository jpaRepository;

	public EmergencyContactRepositoryAdapter(final EmergencyContactJpaRepository jpaRepository) {
		super(jpaRepository, UserPersistenceMapper::toEntity, UserPersistenceMapper::toDomain, Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<EmergencyContact> findByUserId(final Long userId) {
		return jpaRepository.findByUserId(userId).map(UserPersistenceMapper::toDomain);
	}

	@Override
	public void deleteByUserId(final Long userId) {
		jpaRepository.deleteByUserId(userId);
	}
}
