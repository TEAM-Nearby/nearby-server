// 진행 중인 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.port.in.ReadOngoingCompanionMeetingsUseCase;
import com.sopt.nearby.companion.port.out.OngoingCompanionMeetingQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class ReadOngoingCompanionMeetingsService implements ReadOngoingCompanionMeetingsUseCase {

    private final OngoingCompanionMeetingQueryPort queryPort;
    private final Clock clock;
    private final CompanionPlaceTimeResolver placeTimeResolver;

    public ReadOngoingCompanionMeetingsService(
            final OngoingCompanionMeetingQueryPort queryPort,
            final Clock clock
    ) {
        this(queryPort, clock, new CompanionPlaceTimeResolver());
    }

    public ReadOngoingCompanionMeetingsService(
            final OngoingCompanionMeetingQueryPort queryPort,
            final Clock clock,
            final CompanionPlaceTimeResolver placeTimeResolver
    ) {
        this.queryPort = queryPort;
        this.clock = clock;
        this.placeTimeResolver = placeTimeResolver;
    }

    @Override
    public List<OngoingCompanionMeetingSummary> getOngoingMeetings(final Long userId) {
        final Instant now = clock.instant();
        return queryPort.findAllByParticipantUserId(userId)
                .stream()
                .filter(summary -> !isExpiredWithoutCheckIn(summary, now))
                .map(summary -> withCurrentLocalTime(summary, now))
                .toList();
    }

    private boolean isExpiredWithoutCheckIn(
            final OngoingCompanionMeetingSummary summary,
            final Instant now
    ) {
        final LocalDateTime currentTime = LocalDateTime.ofInstant(now, java.time.ZoneOffset.UTC);
        return !summary.checkedIn()
                && summary.progressStatus() == CompanionMeetingProgressStatus.ONGOING
                && summary.meetingAt() != null
                && currentTime != null
                && currentTime.isAfter(summary.meetingAt().plusHours(1));
    }

    private OngoingCompanionMeetingSummary withCurrentLocalTime(
            final OngoingCompanionMeetingSummary summary,
            final Instant now
    ) {
        final ResolvedCityTime cityTime = placeTimeResolver.resolveCurrentTime(
                summary.placeAddress(),
                summary.latitude(),
                summary.longitude(),
                now
        );
        return new OngoingCompanionMeetingSummary(
                summary.meetingId(),
                summary.matchId(),
                summary.companion(),
                summary.placeName(),
                summary.placeAddress(),
                summary.latitude(),
                summary.longitude(),
                cityTime.city(),
                cityTime.currentLocalTime(),
                CompanionMeetingTimeConverter.toPlaceLocalTime(
                        summary.meetingAt(),
                        cityTime.currentLocalTime().getZone()
                ),
                summary.meetingTimeType(),
                summary.checkedIn(),
                summary.meetingStatus(),
                summary.progressStatus()
        );
    }

}
