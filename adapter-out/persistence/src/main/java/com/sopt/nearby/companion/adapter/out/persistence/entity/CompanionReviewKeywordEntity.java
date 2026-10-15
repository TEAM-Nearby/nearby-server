// 동행 리뷰 키워드 테이블을 매핑하는 JPA 엔티티
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "companion_review_keyword")
@IdClass(CompanionReviewKeywordEntityId.class)
public class CompanionReviewKeywordEntity {

	@Id
	@Column(name = "review_id", nullable = false)
	private Long reviewId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ReviewKeyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "review_id", insertable = false, updatable = false)
	private CompanionReviewEntity review;

	protected CompanionReviewKeywordEntity() {
	}

	public CompanionReviewKeywordEntity(final Long reviewId, final ReviewKeyword keyword) {
		this.reviewId = reviewId;
		this.keyword = keyword;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public ReviewKeyword getKeyword() {
		return keyword;
	}
}
