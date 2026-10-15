// 동행 모집글 목록 조회 결과를 표현한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.post.CompanionPostPlaceCategory;
import com.sopt.nearby.companion.domain.model.post.CompanionPostSort;
import com.sopt.nearby.companion.domain.model.post.CompanionPostStatus;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionPostsResult(
        CurrentLocation currentLocation,
        int radiusMeters,
        int maxRadiusMeters,
        CompanionPostPlaceCategory placeCategory,
        CompanionPostSort sort,
        int totalCount,
        String summaryText,
        List<Post> posts
) {

    public record CurrentLocation(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
    }

    public record Post(
            Long postId,
            CompanionPostStatus status,
            Host host,
            Place place,
            String contentPreview,
            boolean contentPreviewTruncated,
            CompanionPostMeetingTimeType meetingTimeType,
            LocalDateTime meetingAt,
            String meetingAtText,
            int participantCount,
            int maxParticipants,
            List<Participant> participants,
            String participantSummaryText,
            LocalDateTime createdAt,
            String createdAgoText,
            String mapMarkerText
    ) {
        public Post {
            participants = participants == null ? List.of() : List.copyOf(participants);
        }
    }

    public record Participant(
            Long userId,
            String profileImageUrl
    ) {
    }

    public record Host(
            String nickname,
            UserGender gender
    ) {
    }

    public record Place(
            Long placeId,
            String googlePlaceId,
            String name,
            CompanionPostPlaceCategory category,
            BigDecimal latitude,
            BigDecimal longitude,
            int distanceMeters,
            String imageUrl,
            String imageSource,
            List<ImageAttribution> imageAttributions
    ) {
    }

    public record ImageAttribution(
            String displayName,
            String uri,
            String photoUri
    ) {
    }
}
