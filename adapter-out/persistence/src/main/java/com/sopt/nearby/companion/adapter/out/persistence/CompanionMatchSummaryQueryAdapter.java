// 매칭된 동행 목록 조회 쿼리 결과를 애플리케이션 조회 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionMatchSummaryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionMatchSummaryQueryPort;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionMatchSummaryQueryAdapter implements CompanionMatchSummaryQueryPort {

    private final CompanionMatchSummaryJpaRepository repository;

    public CompanionMatchSummaryQueryAdapter(final CompanionMatchSummaryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CompanionMatchSummary> findAllByParticipantUserId(final Long userId) {
        return repository.findAllByParticipantUserId(userId)
                .stream()
                .map(row -> new CompanionMatchSummary(
                        row.getMatchId(),
                        row.getHostNickname(),
                        row.getHostProfileImageUrl(),
                        UserGender.valueOf(row.getHostGender()),
                        row.getPlaceName(),
                        row.getMeetingAt(),
                        CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
                        row.getCreatedAt(),
                        row.getContent(),
                        CompanionMatchStatus.valueOf(row.getMatchStatus())
                ))
                .toList();
    }

    @Override
    public Optional<String> findPlaceNameByMatchId(final Long matchId) {
        return repository.findPlaceNameByMatchId(matchId);
    }
}
