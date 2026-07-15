// 동행 일정 수정 요청 값을 애플리케이션 계층으로 전달하는 명령 객체
package com.sopt.nearby.companion.application;

import java.time.LocalDateTime;

public record ConfirmCompanionScheduleCommand(
        Long matchId,
        Long requesterUserId,
        LocalDateTime scheduledAt
) {
}
