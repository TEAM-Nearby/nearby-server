// 동행 신고 이메일에 필요한 동행 정보를 조회하는 포트
package com.sopt.nearby.companion.port.out;

import java.util.Optional;

public interface CompanionReportMailContextQueryPort {

	Optional<CompanionReportMailContext> findByMeetingIdAndUserIds(
			Long meetingId,
			Long reporterUserId,
			Long reportedUserId
	);
}
