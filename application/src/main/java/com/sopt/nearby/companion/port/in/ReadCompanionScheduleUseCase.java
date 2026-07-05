// 내 동행 일정 조회용 유스 케이스
package com.sopt.nearby.companion.port.in;

import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;

public interface ReadCompanionScheduleUseCase {
    CompanionScheduleDetail getSchedule(Long matchId, Long userId);
}
