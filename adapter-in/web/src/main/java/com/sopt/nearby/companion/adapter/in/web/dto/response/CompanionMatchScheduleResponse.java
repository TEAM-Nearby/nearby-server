// 동행 일정 수정 성공 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ConfirmCompanionScheduleResult;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;

public record CompanionMatchScheduleResponse(
        Long matchId,
        Long scheduleId,
        CompanionMatchStatus matchStatus
) {
    public static CompanionMatchScheduleResponse from(final ConfirmCompanionScheduleResult result) {
        return new CompanionMatchScheduleResponse(
                result.matchId(),
                result.scheduleId(),
                result.matchStatus()
        );
    }
}
