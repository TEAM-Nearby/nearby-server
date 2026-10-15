// 동행 만남 인증 요청 본문을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.CheckInCompanionMeetingCommand;
import java.math.BigDecimal;

public record CheckInCompanionMeetingRequest(
        BigDecimal latitude,
        BigDecimal longitude
) {

    public CheckInCompanionMeetingCommand toCommand(final Long meetingId, final Long userId) {
        return new CheckInCompanionMeetingCommand(userId, meetingId, latitude, longitude);
    }
}
