// 동행 리뷰 키워드를 정의하는 enum
package com.sopt.nearby.companion.domain.model.review;

public enum ReviewKeyword {
	FAST_RESPONSE(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION, "연락이 빨라요"),
	GOOD_MANNERS(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION, "매너가 좋아요"),
	GOOD_CONVERSATION(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION, "대화가 잘 통해요"),
	GOOD_TALKER(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION, "입담이 좋아요"),
	INFORMATIVE(CompanionReviewKeywordCategory.CONSIDERATION_COMMUNICATION, "유용한 정보를 많이 알아요"),
	PUNCTUAL(CompanionReviewKeywordCategory.TIME_PROMISE, "시간 약속을 잘 지켜요"),
	NOTIFY_DELAY_IN_ADVANCE(CompanionReviewKeywordCategory.TIME_PROMISE, "늦어도 미리 알려줘요"),
	ARRIVES_EARLY(CompanionReviewKeywordCategory.TIME_PROMISE, "약속 시간보다 일찍 와요");

	private final CompanionReviewKeywordCategory category;
	private final String description;

	ReviewKeyword(final CompanionReviewKeywordCategory category, final String description) {
		this.category = category;
		this.description = description;
	}

	public CompanionReviewKeywordCategory getCategory() {
		return category;
	}

	public String getDescription() {
		return description;
	}
}
