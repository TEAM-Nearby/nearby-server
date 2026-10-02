// 동행 신고 보고서에 필요한 저장소 조회 결과를 표현하는 모델
package com.sopt.nearby.companion.port.out;

import java.time.LocalDateTime;

public record CompanionReportMailContext(
		String reporterNickname,
		String reporterPhoneNumber,
		String reportedNickname,
		String reportedPhoneNumber,
		Long companionPostId,
		String companionPostContent,
		LocalDateTime scheduledAt,
		String placeAddress
) {
}
