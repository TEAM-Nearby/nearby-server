// 동행 리뷰 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReviewEntity;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionReview;
import com.sopt.nearby.domain.companion.repository.CompanionReviewRepository;
import java.util.function.Function;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReviewRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReview, Long, CompanionReviewEntity, Long>
		implements CompanionReviewRepository {

	public CompanionReviewRepositoryAdapter(final CompanionReviewJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				Function.identity());
	}
}
