// 만남 인증 조회 포트를 JPA 네이티브 쿼리로 구현하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.projection.CompanionMeetingCheckInProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingCheckInQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingCheckInContext;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.port.out.CompanionMeetingCheckInQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMeetingCheckInQueryAdapter implements CompanionMeetingCheckInQueryPort {

    private final CompanionMeetingCheckInQueryJpaRepository jpaRepository;

    public CompanionMeetingCheckInQueryAdapter(final CompanionMeetingCheckInQueryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<CompanionMeetingCheckInContext> findByMeetingId(final Long meetingId) {
        return jpaRepository.findContextByMeetingId(meetingId)
                .map(this::toDomain);
    }

    private CompanionMeetingCheckInContext toDomain(final CompanionMeetingCheckInProjection row) {
        return new CompanionMeetingCheckInContext(
                row.getMeetingId(),
                row.getMatchId(),
                CompanionMeetingStatus.valueOf(row.getMeetingStatus()),
                row.getScheduleId(),
                row.getPlaceId(),
                row.getScheduledAt(),
                row.getPlaceLatitude(),
                row.getPlaceLongitude()
        );
    }
}
