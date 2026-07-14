// 동행 신청 검토 화면 상세 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompanionRequestReviewResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionRequestReviewResponse(
        Long applicationId,
        Long postId,
        String applicationStatus,
        String placeName,
        String meetingTimeType,
        LocalDateTime meetingAt,
        ApplicantProfileResponse applicantProfile,
        ApplicantAccountResponse applicantAccount
) {

    public static CompanionRequestReviewResponse from(final CompanionRequestReviewResult result) {
        return new CompanionRequestReviewResponse(
                result.applicationId(),
                result.postId(),
                result.applicationStatus().name(),
                result.placeName(),
                result.meetingTimeType().name(),
                result.meetingAt(),
                ApplicantProfileResponse.from(result.applicantProfile()),
                ApplicantAccountResponse.from(result.applicantAccount())
        );
    }

    public record ApplicantProfileResponse(
            String profileImageUrl,
            String nickname,
            String gender,
            Integer birthYear,
            BigDecimal mannerScore
    ) {
        static ApplicantProfileResponse from(final CompanionRequestReviewResult.ApplicantProfile profile) {
            return new ApplicantProfileResponse(
                    profile.profileImageUrl(),
                    profile.nickname(),
                    profile.gender().name(),
                    profile.birthYear(),
                    profile.mannerScore()
            );
        }
    }

    public record ApplicantAccountResponse(
            LocalDateTime phoneVerifiedAt
    ) {
        static ApplicantAccountResponse from(final CompanionRequestReviewResult.ApplicantAccount account) {
            return new ApplicantAccountResponse(account.phoneVerifiedAt());
        }
    }
}
