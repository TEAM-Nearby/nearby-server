// 동행 후기 작성 대상 정보를 표현하는 조회 모델
package com.sopt.nearby.companion.domain.model.review;

import java.time.LocalDate;

public record CompanionReviewTarget(
		Long revieweeUserId,
		String profileImageUrl,
		String nickname,
		String cityName,
		LocalDate meetingDate,
		boolean checkedIn,
		boolean hasWrittenReview
) {
}
