// 내 동행 일정 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.port.in.ReadCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;
import java.time.Clock;

public class ReadCompanionScheduleService implements ReadCompanionScheduleUseCase {
    private final CompanionScheduleDetailQueryPort companionScheduleDetailQueryPort;
    private final Clock clock;

    public ReadCompanionScheduleService(
            final CompanionScheduleDetailQueryPort companionScheduleDetailQueryPort,
            final Clock clock
    ) {
        this.companionScheduleDetailQueryPort = companionScheduleDetailQueryPort;
        this.clock = clock;
    }

    @Override
    public CompanionScheduleDetail getSchedule(Long matchId, Long userId) {
        if (matchId == null || matchId <= 0) {
            throw new InvalidCompanionMatchIdException();
        }
        CompanionScheduleDetail scheduleDetail = companionScheduleDetailQueryPort
                .findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(CompanionMatchNotFoundException::new);
        if (scheduleDetail.currentUserRole() == null) {
            throw new ForbiddenReadCompanionScheduleException();
        }

        if (scheduleDetail.matchStatus() == CompanionMatchStatus.CANCELED) {
            throw new CompanionMatchScheduleNotReadableException();
        }

        String placeAddress = scheduleDetail.schedule() == null || scheduleDetail.schedule().place() == null
                ? null
                : scheduleDetail.schedule().place().address();
        ResolvedCityTime cityTime = CompanionPlaceCityNameResolver.resolveCurrentTime(
                placeAddress,
                clock.instant()
        );
        return new CompanionScheduleDetail(
                scheduleDetail.matchId(),
                scheduleDetail.matchStatus(),
                cityTime.city(),
                cityTime.currentLocalTime(),
                scheduleDetail.schedule(),
                scheduleDetail.openChatUrl(),
                scheduleDetail.userNickname(),
                scheduleDetail.meetingTimeType(),
                scheduleDetail.currentUserRole()
        );
    }
}
