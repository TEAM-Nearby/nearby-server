// 동행 리뷰 키워드를 정의하는 enum
package com.sopt.nearby.companion.domain.model.review;

public enum ReviewKeyword {
	FAST_RESPONSE(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION),
	GOOD_MANNERS(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION),
	GOOD_CONVERSATION(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION),
	GOOD_TALKER(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION),
	INFORMATIVE(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION),
	PUNCTUAL(CompanionReviewKeywordCategory.TIME_PROMISE),
	NOTIFY_DELAY_IN_ADVANCE(CompanionReviewKeywordCategory.TIME_PROMISE),
	ARRIVES_EARLY(CompanionReviewKeywordCategory.TIME_PROMISE);

	private final CompanionReviewKeywordCategory category;

	ReviewKeyword(final CompanionReviewKeywordCategory category) {
		this.category = category;
	}

	public CompanionReviewKeywordCategory getCategory() {
		return category;
	}
}
