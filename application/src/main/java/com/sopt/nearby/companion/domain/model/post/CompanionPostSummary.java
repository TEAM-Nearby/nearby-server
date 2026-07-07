// 동행 모집글 조회 쿼리 결과를 표현한다.
package com.sopt.nearby.companion.domain.model.post;

import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionPostSummary(
        Long postId,
        CompanionPostStatus status,
        String hostNickname,
        UserGender hostGender,
        Long placeId,
        String googlePlaceId,
        String placeName,
        CompanionPostPlaceCategory placeCategory,
        BigDecimal latitude,
        BigDecimal longitude,
        int distanceMeters,
        String photoReference,
        String content,
        LocalDateTime meetingAt,
        int participantCount,
        int maxParticipants,
        LocalDateTime createdAt
) {
}
