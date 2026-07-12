// 수락된 동행 신청 상세 조회 결과를 애플리케이션 모델로 변환하는 어댑터
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.AcceptedCompanionRequestDetailJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.AcceptedCompanionRequestDetailProjection;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.port.out.AcceptedCompanionRequestDetailQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AcceptedCompanionRequestDetailQueryAdapter implements AcceptedCompanionRequestDetailQueryPort {

    private final AcceptedCompanionRequestDetailJpaRepository repository;

    public AcceptedCompanionRequestDetailQueryAdapter(final AcceptedCompanionRequestDetailJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<AcceptedCompanionRequestDetail> findByApplicationIdAndRequesterUserId(
            final Long applicationId,
            final Long requesterUserId
    ) {
        return repository.findByApplicationIdAndRequesterUserId(applicationId, requesterUserId)
                .map(this::toDetail);
    }

    private AcceptedCompanionRequestDetail toDetail(final AcceptedCompanionRequestDetailProjection row) {
        return new AcceptedCompanionRequestDetail(
                row.getMatchId(),
                CompanionMatchStatus.valueOf(row.getMatchStatus()),
                row.getPostId(),
                new AcceptedCompanionRequestDetail.Host(
                        row.getHostUserId(),
                        row.getHostNickname(),
                        row.getHostProfileImageUrl()
                ),
                new AcceptedCompanionRequestDetail.Place(
                        row.getGooglePlaceId(),
                        row.getPlaceName(),
                        row.getPlaceAddress(),
                        row.getPlaceLatitude(),
                        row.getPlaceLongitude()
                ),
                CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
                row.getMeetingAt(),
                row.getParticipantCount(),
                row.getMaxParticipants(),
                row.getOpenChatUrl()
        );
    }
}
