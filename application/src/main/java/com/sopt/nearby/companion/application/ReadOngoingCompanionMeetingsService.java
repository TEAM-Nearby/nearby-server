// 진행 중인 동행 목록 조회 유스케이스를 구현하는 서비스
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.port.in.ReadOngoingCompanionMeetingsUseCase;
import com.sopt.nearby.companion.port.out.OngoingCompanionMeetingQueryPort;
import java.util.List;

public class ReadOngoingCompanionMeetingsService implements ReadOngoingCompanionMeetingsUseCase {

    private final OngoingCompanionMeetingQueryPort queryPort;

    public ReadOngoingCompanionMeetingsService(final OngoingCompanionMeetingQueryPort queryPort) {
        this.queryPort = queryPort;
    }

    @Override
    public List<OngoingCompanionMeetingSummary> getOngoingMeetings(final Long userId) {
        return queryPort.findAllByParticipantUserId(userId);
    }
}
