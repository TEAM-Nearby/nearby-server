// 내 동행 일정 조회 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleDetailJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionScheduleDetail;
import com.sopt.nearby.companion.port.out.CompanionScheduleDetailQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionScheduleDetailQueryAdapter implements CompanionScheduleDetailQueryPort {

    private final CompanionScheduleDetailJpaRepository repository;

    public CompanionScheduleDetailQueryAdapter(final CompanionScheduleDetailJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanionScheduleDetail> findByMatchId(final Long matchId) {
        return repository.findByMatchId(matchId)
                .map(row -> new CompanionScheduleDetail(
                        row.getMatchId(),
                        CompanionMatchStatus.valueOf(row.getMatchStatus()),
                        toSchedule(row),
                        row.getOpenChatUrl()
                ));
    }

    private CompanionScheduleDetail.Schedule toSchedule(
            final com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionScheduleDetailProjection row
    ) {
        if (row.getScheduleId() == null) {
            return null;
        }

        return new CompanionScheduleDetail.Schedule(
                row.getScheduleId(),
                new CompanionScheduleDetail.Place(
                        row.getGooglePlaceId(),
                        row.getPlaceName(),
                        row.getPlaceAddress(),
                        row.getLatitude(),
                        row.getLongitude()
                ),
                row.getScheduledAt()
        );
    }
}