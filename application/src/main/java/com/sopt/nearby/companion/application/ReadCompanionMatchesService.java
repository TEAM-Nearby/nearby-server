// 매칭된 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import java.util.List;

public class ReadCompanionMatchesService implements ReadCompanionMatchesUseCase {

    private final CompanionMatchSummaryQueryPort queryPort;

    public ReadCompanionMatchesService(final CompanionMatchSummaryQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<CompanionMatchSummary> getMatches(final Long userId) {
        return queryPort.findAllByParticipantUserId(userId);
    }
}