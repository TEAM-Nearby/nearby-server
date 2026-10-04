// 진행 중인 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingProgressStatus;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.in.ReadOngoingCompanionMeetingsUseCase;
import com.sopt.nearby.companion.port.out.OngoingCompanionMeetingQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public class ReadOngoingCompanionMeetingsService implements ReadOngoingCompanionMeetingsUseCase {

    private final OngoingCompanionMeetingQueryPort queryPort;
    private final Clock clock;

    public ReadOngoingCompanionMeetingsService(
            final OngoingCompanionMeetingQueryPort queryPort,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.clock = clock;
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
        final LocalDateTime currentTime = currentTimeForExpiry(summary, now);
        return !summary.checkedIn()
                && summary.progressStatus() == CompanionMeetingProgressStatus.ONGOING
                && summary.meetingAt() != null
                && currentTime != null
                && currentTime.isAfter(summary.meetingAt().plusHours(1));
    }

    private LocalDateTime currentTimeForExpiry(
            final OngoingCompanionMeetingSummary summary,
            final Instant now
    ) {
        if (summary.meetingTimeType() == CompanionPostMeetingTimeType.NOW) {
            return LocalDateTime.ofInstant(now, clock.getZone());
        }
        if (summary.meetingTimeType() == CompanionPostMeetingTimeType.SCHEDULED) {
            final ResolvedCityTime cityTime = CompanionPlaceCityNameResolver.resolveCurrentTime(
                    summary.placeAddress(),
                    now
            );
            return cityTime.currentLocalTime() == null
                    ? null
                    : cityTime.currentLocalTime().toLocalDateTime();
        }
        return null;
    }

    private OngoingCompanionMeetingSummary withCurrentLocalTime(
            final OngoingCompanionMeetingSummary summary,
            final Instant now
    ) {
        final ResolvedCityTime cityTime = CompanionPlaceCityNameResolver.resolveCurrentTime(
                summary.placeAddress(),
                now
        );
        return new OngoingCompanionMeetingSummary(
                summary.meetingId(),
                summary.matchId(),
                summary.companion(),
                summary.placeName(),
                summary.placeAddress(),
                cityTime.city(),
                cityTime.currentLocalTime(),
                summary.meetingAt(),
                summary.meetingTimeType(),
                summary.checkedIn(),
                summary.meetingStatus(),
                summary.progressStatus()
        );
    }
}
