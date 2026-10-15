// 동행 후기 대상 목록 조회 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import java.util.List;

public record ReadCompanionReviewTargetsResult(
		CompanionMeetingStatus meetingStatus,
		MatchParticipantRole currentUserRole,
		boolean canCompleteMeeting,
		List<CompanionReviewTarget> reviewTargets
) {
}
