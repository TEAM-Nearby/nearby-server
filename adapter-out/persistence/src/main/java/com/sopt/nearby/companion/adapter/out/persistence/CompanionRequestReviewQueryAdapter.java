// 동행 신청 검토 화면 상세 조회 쿼리 포트를 JPA로 구현한다.
package com.sopt.nearby.companion.adapter.out.persistence;

import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionRequestReviewProjection;
import com.sopt.nearby.companion.adapter.out.persistence.repository.CompanionRequestReviewQueryJpaRepository;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.port.out.CompanionRequestReviewQueryPort;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class CompanionRequestReviewQueryAdapter implements CompanionRequestReviewQueryPort {

    private final CompanionRequestReviewQueryJpaRepository repository;

    public CompanionRequestReviewQueryAdapter(final CompanionRequestReviewQueryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<CompanionRequestReview> findByApplicationId(final Long applicationId) {
        return repository.findReviewByApplicationId(applicationId)
                .map(this::toReview);
    }

    private CompanionRequestReview toReview(final CompanionRequestReviewProjection row) {
        return new CompanionRequestReview(
                row.getApplicationId(),
                row.getPostId(),
                CompanionApplicationStatus.valueOf(row.getApplicationStatus()),
                row.getHostUserId(),
                row.getPlaceName(),
                CompanionPostMeetingTimeType.valueOf(row.getMeetingTimeType()),
                row.getMeetingAt(),
                row.getExposureExpiresAt(),
                new CompanionRequestReview.ApplicantProfile(
                        row.getApplicantProfileId(),
                        row.getApplicantProfileImageUrl(),
                        row.getApplicantNickname(),
                        UserGender.valueOf(row.getApplicantGender()),
                        row.getApplicantBirthYear(),
                        row.getApplicantMannerScore()
                ),
                new CompanionRequestReview.ApplicantAccount(row.getApplicantPhoneVerifiedAt())
        );
    }
}
