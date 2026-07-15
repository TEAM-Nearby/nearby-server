// 동행 신청 검토 화면 조회에 필요한 조인 결과를 표현한다.
package com.sopt.nearby.companion.domain.model.match;

import com.sopt.nearby.companion.domain.model.profile.UserGender;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionRequestReview(
        Long applicationId,
        Long postId,
        CompanionApplicationStatus applicationStatus,
        Long hostUserId,
        String placeName,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt,
        LocalDateTime exposureExpiresAt,
        ApplicantProfile applicantProfile,
        ApplicantAccount applicantAccount
) {

    public record ApplicantProfile(
            Long profileId,
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
