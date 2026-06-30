// 동행 리뷰 키워드 도메인 저장소 포트를 JPA로 구현하는 어댑터
package com.sopt.nearby.adapter.persistence.companion;

import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.adapter.persistence.companion.entity.CompanionReviewKeywordEntityId;
import com.sopt.nearby.adapter.persistence.companion.mapper.CompanionPersistenceMapper;
import com.sopt.nearby.adapter.persistence.companion.repository.CompanionReviewKeywordJpaRepository;
import com.sopt.nearby.adapter.persistence.support.SimpleJpaRepositoryAdapter;
import com.sopt.nearby.domain.companion.model.CompanionReviewKeyword;
import com.sopt.nearby.domain.companion.repository.CompanionReviewKeywordRepository;
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
