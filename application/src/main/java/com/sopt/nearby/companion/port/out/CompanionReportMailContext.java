// 동행 신고 보고서에 필요한 저장소 조회 결과를 표현하는 모델
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.time.LocalDateTime;
import java.math.BigDecimal;

public record CompanionReportMailContext(
		String reporterNickname,
		String reporterPhoneNumber,
		String reportedNickname,
		String reportedPhoneNumber,
		Long companionPostId,
		String companionPostContent,
		LocalDateTime scheduledAt,
		CompanionPostMeetingTimeType meetingTimeType,
		String placeAddress,
		BigDecimal latitude,
		BigDecimal longitude
) {
	public CompanionReportMailContext(
			String reporterNickname, String reporterPhoneNumber, String reportedNickname,
			String reportedPhoneNumber, Long companionPostId, String companionPostContent,
			LocalDateTime scheduledAt, CompanionPostMeetingTimeType meetingTimeType, String placeAddress
	) {
		this(reporterNickname, reporterPhoneNumber, reportedNickname, reportedPhoneNumber,
				companionPostId, companionPostContent, scheduledAt, meetingTimeType, placeAddress, null, null);
	}
}
