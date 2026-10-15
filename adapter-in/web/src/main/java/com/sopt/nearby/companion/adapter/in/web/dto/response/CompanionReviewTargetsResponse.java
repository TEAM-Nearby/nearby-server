// 동행 후기 대상 목록을 HTTP 응답으로 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadCompanionReviewTargetsResult;
import java.util.List;

public record CompanionReviewTargetsResponse(
		String meetingStatus,
		String currentUserRole,
		boolean canCompleteMeeting,
		List<CompanionReviewTargetResponse> reviewTargets
) {

	public static CompanionReviewTargetsResponse from(final ReadCompanionReviewTargetsResult result) {
		return new CompanionReviewTargetsResponse(
				result.meetingStatus().name(),
				result.currentUserRole().name(),
				result.canCompleteMeeting(),
				result.reviewTargets()
						.stream()
						.map(CompanionReviewTargetResponse::from)
						.toList()
		);
	}
}
