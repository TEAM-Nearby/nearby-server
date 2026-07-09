// 동행 신청 검토 화면 상세 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionRequestReview;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionRequestReviewResult(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        String placeName,
        LocalDateTime meetingAt,
        ApplicantProfile applicantProfile,
        ApplicantAccount applicantAccount
) {

    public static CompanionRequestReviewResult from(
            final CompanionRequestReview review,
            final LocalDateTime meetingAt
    ) {
        return new CompanionRequestReviewResult(
                review.applicationId(),
                review.postId(),
                review.applicationStatus(),
                review.placeName(),
                meetingAt,
                new ApplicantProfile(
                        review.applicantProfile().profileImageUrl(),
                        review.applicantProfile().nickname(),
                        review.applicantProfile().gender(),
                        review.applicantProfile().birthYear(),
                        review.applicantProfile().mannerScore()
                ),
                new ApplicantAccount(review.applicantAccount().phoneVerifiedAt())
        );
    }

    public record ApplicantProfile(
            String profileImageUrl,
            String nickname,
            UserGender gender,
            Integer birthYear,
            BigDecimal mannerScore
    ) {
    }

    public record ApplicantAccount(
            LocalDateTime phoneVerifiedAt
    ) {
    }
}
