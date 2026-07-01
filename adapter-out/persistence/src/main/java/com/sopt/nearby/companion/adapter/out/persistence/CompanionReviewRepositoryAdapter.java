// 동행 리뷰 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewEntity;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.review.CompanionReview;
import com.sopt.nearby.companion.port.out.CompanionReviewRepository;
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
