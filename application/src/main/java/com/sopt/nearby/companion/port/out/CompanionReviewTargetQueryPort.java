// 동행 후기 대상 목록 조회 데이터를 가져오는 포트
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.review.CompanionReviewTarget;
import java.util.List;

public interface CompanionReviewTargetQueryPort {

	List<CompanionReviewTarget> findAllByMeetingIdAndReviewerUserIdAndTargetRole(
			Long meetingId,
			Long reviewerUserId,
			MatchParticipantRole targetRole
	);
}
