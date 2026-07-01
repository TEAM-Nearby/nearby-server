// 동행 리뷰 키워드 JPA 저장소를 정의하는 인터페이스
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntity;
import com.sopt.nearby.companion.adapter.out.persistence.entity.CompanionReviewKeywordEntityId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanionReviewKeywordJpaRepository
		extends JpaRepository<CompanionReviewKeywordEntity, CompanionReviewKeywordEntityId> {
}
