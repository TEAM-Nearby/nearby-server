// 내 동행 일정 조회용 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import java.util.Optional;

public interface CompanionScheduleDetailQueryPort {
    Optional<CompanionScheduleDetail> findByMatchIdAndUserId(Long matchId, Long userId);
}
