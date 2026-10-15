// 동행 리뷰 키워드의 평가 영역을 정의하는 enum
package com.sopt.nearby.companion.domain.model.review;

public enum CompanionReviewKeywordCategory {
	CONSIDERATION_COMMUNICATION("배려 소통"),
	TIME_PROMISE("시간 약속");

	private final String description;

	CompanionReviewKeywordCategory(final String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
}
