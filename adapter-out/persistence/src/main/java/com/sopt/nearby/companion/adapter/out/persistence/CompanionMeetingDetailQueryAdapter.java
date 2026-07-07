// 동행 상세 조회 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingDetailProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMeetingDetailQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.MatchParticipantRole;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingDetail;
import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionMeetingDetailQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMeetingDetailQueryAdapter implements CompanionMeetingDetailQueryPort {

    private final CompanionMeetingDetailQueryJpaRepository repository;

    public CompanionMeetingDetailQueryAdapter(final CompanionMeetingDetailQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanionMeetingDetail> findByMeetingIdAndUserId(final Long meetingId, final Long userId) {
        return repository.findByMeetingIdAndUserId(meetingId, userId)
                .map(this::toDetail);
    }

    private CompanionMeetingDetail toDetail(final CompanionMeetingDetailProjection row) {
        return new CompanionMeetingDetail(
                row.getMeetingId(),
                toRole(row.getCurrentUserRole()),
                row.getHostId(),
                UserGender.valueOf(row.getHostGender()),
                row.getHostProfileImageUrl(),
                row.getHostNickname(),
                Boolean.TRUE.equals(row.getHostCheckedIn()),
                row.getPlaceName(),
                row.getMeetingAt(),
                CompanionMeetingStatus.valueOf(row.getMeetingStatus()),
                Boolean.TRUE.equals(row.getCurrentUserCheckedIn())
        );
    }

    //NPE를 막기 위해 Null을 처리
    private MatchParticipantRole toRole(final String role) {
        if (role == null) {
            return null;
        }
        return MatchParticipantRole.valueOf(role);
    }
}
