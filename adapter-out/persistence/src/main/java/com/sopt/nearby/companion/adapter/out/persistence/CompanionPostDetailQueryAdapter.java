// 동행 모집 글 상세 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostDetailProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostDetailQueryJpaRepository;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostParticipantProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionPostQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostDetail;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import com.sopt.nearby.companion.port.out.CompanionPostDetailQueryPort;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionPostDetailQueryAdapter implements CompanionPostDetailQueryPort {

    private final CompanionPostDetailQueryJpaRepository repository;
    private final CompanionPostQueryJpaRepository postQueryRepository;

    public CompanionPostDetailQueryAdapter(
            final CompanionPostDetailQueryJpaRepository repository,
            final CompanionPostQueryJpaRepository postQueryRepository
    ) {
        this.repository = repository;
        this.postQueryRepository = postQueryRepository;
    }

    @Override
    public Optional<CompanionPostDetail> findByPostId(final Long postId, final Long userId) {
        return repository.findDetailByPostId(postId, userId)
                .map(row -> toDetail(
                        row,
                        repository.findKeywordsByProfileId(row.getHostProfileId()),
                        postQueryRepository.findParticipantsByPostIds(List.of(postId))
                ));
    }

    private CompanionPostDetail toDetail(
            final CompanionPostDetailProjection row,
            final List<TravelStyleKeyword> keywords,
            final List<CompanionPostParticipantProjection> participants
    ) {
        return new CompanionPostDetail(
                row.getPostId(),
                row.getHostUserId(),
                row.getMeetingAt(),
                row.getMaxParticipants().intValue(),
                row.getContent(),
                row.getOpenChatUrl(),
                CompanionPostStatus.valueOf(row.getStatus()),
                row.getCreatedAt(),
                CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
                row.getExpiresAt(),
                row.getParticipantCount().intValue(),
                participants.stream()
                        .map(participant -> new CompanionPostDetail.Participant(
                                participant.getUserId(),
                                participant.getProfileImageUrl()
                        ))
                        .toList(),
                toApplicationStatus(row.getApplicationStatus()),
                new CompanionPostDetail.Place(
                        row.getGooglePlaceId(),
                        row.getPlaceName(),
                        row.getPlaceAddress(),
                        row.getLatitude(),
                        row.getLongitude(),
                        toPlaceCategory(row.getPlaceCategory())
                ),
                new CompanionPostDetail.HostProfileSummary(
                        row.getHostProfileId(),
                        row.getHostNickname(),
                        UserGender.valueOf(row.getHostGender()),
                        row.getHostBirthYear(),
                        row.getHostProfileImageUrl(),
                        row.getHostMannerScore(),
                        row.getHostPhoneVerifiedAt(),
                        keywords
                )
        );
    }

    private CompanionApplicationStatus toApplicationStatus(final String value) {
        return value == null ? null : CompanionApplicationStatus.valueOf(value);
    }

    private CompanionPostPlaceCategory toPlaceCategory(final String value) {
        try {
            return CompanionPostPlaceCategory.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CompanionPostPlaceCategory.OTHER;
        }
    }
}
