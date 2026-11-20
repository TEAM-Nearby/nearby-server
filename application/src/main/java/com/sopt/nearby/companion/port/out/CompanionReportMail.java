// 동행 신고 이메일에 필요한 정보를 표현하는 발송 모델
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.report.CompanionReport;
import com.sopt.nearby.companion.domain.model.report.ReportReason;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public record CompanionReportMail(
		CompanionReport report,
		List<ReportReason> reasons,
		String reporterNickname,
		String reporterPhoneNumber,
		String reportedNickname,
		String reportedPhoneNumber,
		Long companionPostId,
		String companionPostContent,
		LocalDateTime scheduledAt,
		CompanionPostMeetingTimeType meetingTimeType,
		String placeAddress,
		ZoneId placeZone
) {
	public CompanionReportMail(
			CompanionReport report, List<ReportReason> reasons, String reporterNickname,
			String reporterPhoneNumber, String reportedNickname, String reportedPhoneNumber,
			Long companionPostId, String companionPostContent, LocalDateTime scheduledAt,
			CompanionPostMeetingTimeType meetingTimeType, String placeAddress
	) {
		this(report, reasons, reporterNickname, reporterPhoneNumber, reportedNickname,
				reportedPhoneNumber, companionPostId, companionPostContent, scheduledAt,
				meetingTimeType, placeAddress,
				CompanionPlaceCityNameResolver.resolveSupportedCity(placeAddress)
						.map(city -> city.zoneId()).orElse(ZoneId.of("Asia/Seoul")));
	}
}
