// 동행 모집 글 작성 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostKeyword;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateCompanionPostResult(
        Long postId,
        CompanionPostStatus status,
        Long hostUserId,
        Place place,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt,
        LocalDateTime exposureExpiresAt,
        int maxParticipants,
        int participantCount,
        boolean departEvenIfNotFull,
        List<CompanionPostKeyword> styleKeywords,
        String content,
        String openChatUrl,
        LocalDateTime createdAt
) {

    public record Place(
            Long placeId,
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            CompanionPostPlaceCategory category
    ) {
    }
}
