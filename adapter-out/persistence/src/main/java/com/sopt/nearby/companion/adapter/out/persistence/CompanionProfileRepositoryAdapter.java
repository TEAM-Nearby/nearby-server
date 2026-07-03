// 동행 프로필 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionProfileEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionProfileJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.profile.CompanionProfile;
import com.sopt.nearby.companion.port.out.CompanionProfileRepository;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionProfileRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionProfile, Long, CompanionProfileEntity, Long>
		implements CompanionProfileRepository {

	private final CompanionProfileJpaRepository jpaRepository;

	public CompanionProfileRepositoryAdapter(final CompanionProfileJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public List<CompanionProfile> findAllByUserIdIn(final List<Long> userIds) {
		return jpaRepository.findAllByUserIdIn(userIds)
				.stream()
				.map(CompanionPersistenceMapper::toDomain)
				.toList();
	}
}
