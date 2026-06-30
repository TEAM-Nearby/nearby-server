// 동행 리뷰에 연결된 키워드를 표현하는 도메인 모델
package com.sopt.nearby.domain.companion.model;

public record CompanionReviewKeyword(
		Long reviewId,
		ReviewKeyword keyword
) {

	public Key key() {
		return new Key(reviewId, keyword);
	}

	public record Key(Long reviewId, ReviewKeyword keyword) {
	}
}
