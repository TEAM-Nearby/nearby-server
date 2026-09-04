// 매칭된 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class ReadCompanionMatchesService implements ReadCompanionMatchesUseCase {

    private final CompanionMatchSummaryQueryPort queryPort;
    private final Clock clock;

    public ReadCompanionMatchesService(final CompanionMatchSummaryQueryPort queryPort, final Clock clock) {
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    public List<ReadCompanionMatchResult> getMatches(final Long userId) {
        final Instant now = clock.instant();

        return queryPort.findAllByParticipantUserId(userId)
                .stream()
                .map(match -> CompanionPlaceCityNameResolver.resolveSupportedCity(match.placeAddress())
                        .map(city -> new ReadCompanionMatchResult(match, city, now.atZone(city.zoneId())))
                        .orElseGet(() -> new ReadCompanionMatchResult(match, null, null)))
                .toList();
    }
}
