package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;

public record ConfirmCompanionScheduleResult(
        Long matchId,
        Long scheduleId,
        CompanionMatchStatus matchStatus
) {
}
