// 매칭된 동행 목록 조회용 저장소 포트를 정의하는 인터페이스
package com.sopt.nearby.companion.port.out;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import java.util.List;
import java.util.Optional;

public interface CompanionMatchSummaryQueryPort {
    List<CompanionMatchSummary> findAllByParticipantUserId(Long userId);

    Optional<String> findPlaceNameByMatchId(Long matchId);
}
