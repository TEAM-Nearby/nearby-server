// 동행 리뷰 키워드 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntityId;
import com.sopt.nearby.companion.adapter.out.persistence.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionReviewKeywordJpaRepository;
import com.sopt.nearby.shared.adapter.out.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.companion.domain.model.CompanionReviewKeyword;
import com.sopt.nearby.companion.port.out.CompanionReviewKeywordRepository;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionReviewKeywordRepositoryAdapter
		extends SimpleJpaRepositoryAdapter<CompanionReviewKeyword, CompanionReviewKeyword.Key,
				CompanionReviewKeywordEntity, CompanionReviewKeywordEntityId>
		implements CompanionReviewKeywordRepository {

	public CompanionReviewKeywordRepositoryAdapter(final CompanionReviewKeywordJpaRepository jpaRepository) {
		super(jpaRepository, CompanionPersistenceMapper::toEntity, CompanionPersistenceMapper::toDomain,
				CompanionPersistenceMapper::toEntityId);
	}
}
