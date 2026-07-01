// 동행 리뷰 키워드 엔티티의 복합 키를 표현하는 JPA 식별자 클래스
package com.sopt.nearby.companion.adapter.out.persistence.entity;

import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.io.Serializable;
import java.util.Objects;

public class CompanionReviewKeywordEntityId implements Serializable {

	private Long reviewId;
	private ReviewKeyword keyword;

	public CompanionReviewKeywordEntityId() {
	}

	public CompanionReviewKeywordEntityId(final Long reviewId, final ReviewKeyword keyword) {
		this.reviewId = reviewId;
		this.keyword = keyword;
	}

	public Long getReviewId() {
		return reviewId;
	}

	public ReviewKeyword getKeyword() {
		return keyword;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (!(o instanceof CompanionReviewKeywordEntityId that)) {
			return false;
		}
		return Objects.equals(reviewId, that.reviewId) && keyword == that.keyword;
	}

	@Override
	public int hashCode() {
		return Objects.hash(reviewId, keyword);
	}
}
