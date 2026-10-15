// 동행 일정 수정 요청 본문을 유스케이스 명령으로 변환하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.request;

import com.sopt.nearby.companion.application.ConfirmCompanionScheduleCommand;
import java.time.LocalDateTime;

public record CompanionMatchScheduleRequest(
        LocalDateTime scheduledAt
) {
    public ConfirmCompanionScheduleCommand toCommand(final Long matchId, final Long requesterUserId) {
        return new ConfirmCompanionScheduleCommand(
                matchId,
                requesterUserId,
                scheduledAt
        );
    }
}
