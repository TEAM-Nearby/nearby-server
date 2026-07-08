// 동행 후기 등록 요청 본문을 유스케이스 명령으로 변환하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.CreateCompanionReviewsCommand;
import com.sopt.nearby.companion.domain.exception.InvalidReviewKeywordException;
import com.sopt.nearby.companion.domain.model.review.ReviewKeyword;
import java.util.List;
import java.util.Locale;

public record CreateCompanionReviewsRequest(
		Long revieweeUserId,
		int rating,
		List<String> keywords
) {

	public CreateCompanionReviewsCommand toCommand(final Long meetingId, final Long reviewerUserId) {
		return new CreateCompanionReviewsCommand(
				reviewerUserId,
				meetingId,
				revieweeUserId,
				rating,
				parseKeywords(keywords)
		);
	}

	private List<ReviewKeyword> parseKeywords(final List<String> values) {
		if (values == null) {
			return null;
		}
		return values.stream()
				.map(this::parseKeyword)
				.toList();
	}

	private ReviewKeyword parseKeyword(final String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidReviewKeywordException();
		}
		try {
			return ReviewKeyword.valueOf(value.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new InvalidReviewKeywordException();
		}
	}
}
