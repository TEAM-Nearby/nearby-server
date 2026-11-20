// 매칭된 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

public class ReadCompanionMatchesService implements ReadCompanionMatchesUseCase {

    private final CompanionMatchSummaryQueryPort queryPort;
    private final Clock clock;
    private final CompanionPlaceTimeResolver placeTimeResolver;

    public ReadCompanionMatchesService(final CompanionMatchSummaryQueryPort queryPort, final Clock clock) {
        this(queryPort, clock, new CompanionPlaceTimeResolver());
    }

    public ReadCompanionMatchesService(
            final CompanionMatchSummaryQueryPort queryPort,
            final Clock clock,
            final CompanionPlaceTimeResolver placeTimeResolver
    ) {
        this.queryPort = queryPort;
        this.clock = clock;
        this.placeTimeResolver = placeTimeResolver;
    }

    @Override
    public List<ReadCompanionMatchResult> getMatches(final Long userId) {
        final Instant now = clock.instant();

        return queryPort.findAllByParticipantUserId(userId)
                .stream()
                .map(match -> {
                    var cityTime = placeTimeResolver.resolveCurrentTime(
                            match.placeAddress(), match.latitude(), match.longitude(), now);
                    return new ReadCompanionMatchResult(
                            match,
                            cityTime.city(),
                            cityTime.currentLocalTime(),
                            CompanionMeetingTimeConverter.toPlaceLocalTime(
                                    match.meetingAt(), cityTime.currentLocalTime().getZone())
                    );
                })
                .toList();
    }
}
