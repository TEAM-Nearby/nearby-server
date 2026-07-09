// 진행 중인 동행 목록 조회 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.OngoingCompanionMeetingProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.OngoingCompanionMeetingQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingHostProfile;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.OngoingCompanionMeetingQueryPort;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class OngoingCompanionMeetingQueryAdapter implements OngoingCompanionMeetingQueryPort {

    private final OngoingCompanionMeetingQueryJpaRepository repository;

    public OngoingCompanionMeetingQueryAdapter(final OngoingCompanionMeetingQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OngoingCompanionMeetingSummary> findAllByParticipantUserId(final Long userId) {
        return repository.findAllByParticipantUserId(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    private OngoingCompanionMeetingSummary toSummary(final OngoingCompanionMeetingProjection row) {
        return new OngoingCompanionMeetingSummary(
                row.getMeetingId(),
                row.getMatchId(),
                new OngoingCompanionMeetingHostProfile(
                        row.getHostUserId(),
                        row.getHostProfileImageUrl(),
                        row.getHostNickname(),
                        UserGender.valueOf(row.getHostGender())
                ),
                row.getPlaceName(),
                row.getMeetingAt(),
                CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
                Boolean.TRUE.equals(row.getCheckedIn()),
                CompanionMeetingStatus.valueOf(row.getMeetingStatus())
        );
    }
}
