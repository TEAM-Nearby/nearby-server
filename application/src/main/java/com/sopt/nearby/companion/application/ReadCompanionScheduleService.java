// 내 동행 일정 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompletedCompanionScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.CompanionMatchScheduleNotReadableException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionScheduleException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMatchIdException;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.port.in.ReadCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.out.CompanionMatchParticipantRepository;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;

public class ReadCompanionScheduleService implements ReadCompanionScheduleUseCase {
    private final CompanionScheduleDetailQueryPort companionScheduleDetailQueryPort;
    private final CompanionMatchParticipantRepository participantRepository;

    public ReadCompanionScheduleService(CompanionScheduleDetailQueryPort companionScheduleDetailQueryPort,
                                        CompanionMatchParticipantRepository participantRepository) {
        this.companionScheduleDetailQueryPort = companionScheduleDetailQueryPort;
        this.participantRepository = participantRepository;
    }

    @Override
    public CompanionScheduleDetail getSchedule(Long matchId, Long userId) {
        if (matchId == null || matchId <= 0) {
            throw new InvalidCompanionMatchIdException();
        }
        CompanionScheduleDetail scheduleDetail = companionScheduleDetailQueryPort
                .findByMatchIdAndUserId(matchId, userId)
                .orElseThrow(CompanionMatchNotFoundException::new);
        if (!participantRepository.existsByMatchIdAndUserId(matchId, userId)) {
            throw new ForbiddenReadCompanionScheduleException();
        }

        if (scheduleDetail.matchStatus() == CompanionMatchStatus.CANCELED) {
            throw new CompanionMatchScheduleNotReadableException();
        }
        if (scheduleDetail.matchStatus() == CompanionMatchStatus.COMPLETED) {
            throw new CompletedCompanionScheduleNotReadableException();
        }

        return scheduleDetail;
    }
}
