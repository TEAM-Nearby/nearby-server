// 동행 모집글 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionPostEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionPostJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionPost;
import com.sopt.nearby.domain.companion.repository.CompanionPostRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionPost, Long, CompanionPostEntity, Long>
		implements CompanionPostRepository {

	public CompanionPostRepositoryAdapter(final CompanionPostJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
