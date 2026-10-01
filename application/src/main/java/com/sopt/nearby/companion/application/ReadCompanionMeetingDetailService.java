// 진행 중인 동행 상세 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.exception.CompanionMeetingNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenReadCompanionMeetingException;
import com.sopt.nearby.companion.domain.exception.InvalidCompanionMeetingIdException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCanceledException;
import com.sopt.nearby.companion.domain.exception.ReadCompanionMeetingAlreadyCompletedException;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver.ResolvedCityTime;
import com.sopt.nearby.companion.port.in.ReadCompanionMeetingDetailUseCase;
import com.sopt.nearby.companion.port.out.CompanionMeetingDetailQueryPort;
import java.time.Clock;

public class ReadCompanionMeetingDetailService implements ReadCompanionMeetingDetailUseCase {

    private final CompanionMeetingDetailQueryPort queryPort;
    private final Clock clock;

    public ReadCompanionMeetingDetailService(
            final CompanionMeetingDetailQueryPort queryPort,
            final Clock clock
    ) {
        this.queryPort = queryPort;
        this.clock = clock;
    }

    @Override
    public ReadCompanionMeetingDetailResult getDetail(final Long meetingId, final Long userId) {
        validateMeetingId(meetingId);

        CompanionMeetingDetail detail = queryPort.findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(CompanionMeetingNotFoundException::new);
        if (detail.currentUserRole() == null) {
            throw new ForbiddenReadCompanionMeetingException();
        }
        validateMeetingStatus(detail.meetingStatus());

        ResolvedCityTime cityTime = CompanionPlaceCityNameResolver.resolveCurrentTime(
                detail.placeAddress(),
                clock.instant()
        );
        return ReadCompanionMeetingDetailResult.from(
                detail,
                cityTime.city(),
                cityTime.currentLocalTime()
        );
    }

    private void validateMeetingId(final Long meetingId) {
        if (meetingId == null || meetingId <= 0) {
            throw new InvalidCompanionMeetingIdException();
        }
    }

    private void validateMeetingStatus(final CompanionMeetingStatus status) {
        if (status == CompanionMeetingStatus.CANCELED) {
            throw new ReadCompanionMeetingAlreadyCanceledException();
        }
        if (status == CompanionMeetingStatus.COMPLETED) {
            throw new ReadCompanionMeetingAlreadyCompletedException();
        }
    }
}
