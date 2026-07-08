// 동행 후기 등록 결과를 HTTP 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CreateCompanionReviewsResult;

public record CreateCompanionReviewsResponse(
		Long meetingId,
		Long reviewId,
		String meetingStatus
) {

	public static CreateCompanionReviewsResponse from(final CreateCompanionReviewsResult result) {
		return new CreateCompanionReviewsResponse(
				result.meetingId(),
				result.reviewId(),
				result.meetingStatus().name()
		);
	}
}
