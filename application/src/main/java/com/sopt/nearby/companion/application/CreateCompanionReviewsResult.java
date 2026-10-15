// 동행 후기 등록 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;

public record CreateCompanionReviewsResult(
		Long meetingId,
		Long reviewId,
		CompanionMeetingStatus meetingStatus
) {
}
