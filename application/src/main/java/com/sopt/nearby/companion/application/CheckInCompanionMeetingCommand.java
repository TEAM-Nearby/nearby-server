// 동행 만남 인증 요청 값을 유스케이스로 전달하는 커맨드
package com.sopt.nearby.companion.application;

import java.math.BigDecimal;

public record CheckInCompanionMeetingCommand(
        Long userId,
        Long meetingId,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
