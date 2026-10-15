// 동행 후기 대상 항목을 HTTP 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import java.time.LocalDate;

public record CompanionReviewTargetResponse(
		Long revieweeUserId,
		String profileImageUrl,
		String nickname,
		String cityName,
		LocalDate meetingDate,
		boolean isCheckedIn,
		boolean hasWrittenReview
) {

	public static CompanionReviewTargetResponse from(final CompanionReviewTarget target) {
		return new CompanionReviewTargetResponse(
				target.revieweeUserId(),
				target.profileImageUrl(),
				target.nickname(),
				target.cityName(),
				target.meetingDate(),
				target.checkedIn(),
				target.hasWrittenReview()
		);
	}
}
