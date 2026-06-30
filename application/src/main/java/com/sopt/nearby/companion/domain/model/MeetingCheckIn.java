// 동행 미팅 체크인 정보를 표현하는 도메인 모델
package com.sopt.nearby.companion.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MeetingCheckIn(
		Long id,
		Long meetingId,
		Long userId,
		BigDecimal latitude,
		BigDecimal longitude,
		LocalDateTime checkedInAt
) {
}
