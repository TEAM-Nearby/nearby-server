// 동행 모집글 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionPostEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.post.CompanionPost;
import com.sopt.nearby.companion.port.out.CompanionPostRepository;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionPost, Long, CompanionPostEntity, Long>
		implements CompanionPostRepository {

	private final CompanionPostJpaRepository jpaRepository;

	public CompanionPostRepositoryAdapter(final CompanionPostJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
		this.jpaRepository = jpaRepository;
	}

	@Override
	public Optional<CompanionPost> findByIdForUpdate(final Long id) {
		return jpaRepository.findByIdForUpdate(id)
				.map(CompanionPersistenceMapper::toDomain);
	}
}
