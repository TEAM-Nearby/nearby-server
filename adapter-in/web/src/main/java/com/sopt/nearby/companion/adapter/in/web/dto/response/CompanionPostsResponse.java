// 동행 모집글 목록 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompanionPostsResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionPostsResponse(
        CurrentLocationResponse currentLocation,
        int radiusMeters,
        int maxRadiusMeters,
        String placeCategory,
        String sort,
        int totalCount,
        String summaryText,
        List<PostResponse> posts
) {

    public static CompanionPostsResponse from(final CompanionPostsResult result) {
        return new CompanionPostsResponse(
                CurrentLocationResponse.from(result.currentLocation()),
                result.radiusMeters(),
                result.maxRadiusMeters(),
                result.placeCategory().name(),
                result.sort().name(),
                result.totalCount(),
                result.summaryText(),
                result.posts().stream()
                        .map(PostResponse::from)
                        .toList()
        );
    }

    public record CurrentLocationResponse(
            BigDecimal latitude,
            BigDecimal longitude
    ) {
        static CurrentLocationResponse from(final CompanionPostsResult.CurrentLocation currentLocation) {
            return new CurrentLocationResponse(currentLocation.latitude(), currentLocation.longitude());
        }
    }

    public record PostResponse(
            Long postId,
            String status,
            HostResponse host,
            PlaceResponse place,
            String contentPreview,
            boolean contentPreviewTruncated,
            LocalDateTime meetingAt,
            String meetingAtText,
            int participantCount,
            int maxParticipants,
            List<ParticipantResponse> participants,
            String participantSummaryText,
            LocalDateTime createdAt,
            String createdAgoText,
            String mapMarkerText
    ) {
        static PostResponse from(final CompanionPostsResult.Post post) {
            return new PostResponse(
                    post.postId(),
                    post.status().name(),
                    HostResponse.from(post.host()),
                    PlaceResponse.from(post.place()),
                    post.contentPreview(),
                    post.contentPreviewTruncated(),
                    post.meetingAt(),
                    post.meetingAtText(),
                    post.participantCount(),
                    post.maxParticipants(),
                    post.participants().stream()
                            .map(ParticipantResponse::from)
                            .toList(),
                    post.participantSummaryText(),
                    post.createdAt(),
                    post.createdAgoText(),
                    post.mapMarkerText()
            );
        }
    }

    public record ParticipantResponse(
            Long userId,
            String profileImageUrl
    ) {
        static ParticipantResponse from(final CompanionPostsResult.Participant participant) {
            return new ParticipantResponse(participant.userId(), participant.profileImageUrl());
        }
    }

    public record HostResponse(
            String nickname,
            String gender
    ) {
        static HostResponse from(final CompanionPostsResult.Host host) {
            return new HostResponse(host.nickname(), host.gender().name());
        }
    }

    public record PlaceResponse(
            Long placeId,
            String googlePlaceId,
            String name,
            String category,
            BigDecimal latitude,
            BigDecimal longitude,
            int distanceMeters,
            String imageUrl,
            String imageSource,
            List<ImageAttributionResponse> imageAttributions
    ) {
        static PlaceResponse from(final CompanionPostsResult.Place place) {
            return new PlaceResponse(
                    place.placeId(),
                    place.googlePlaceId(),
                    place.name(),
                    place.category().name(),
                    place.latitude(),
                    place.longitude(),
                    place.distanceMeters(),
                    place.imageUrl(),
                    place.imageSource(),
                    place.imageAttributions().stream()
                            .map(ImageAttributionResponse::from)
                            .toList()
            );
        }
    }

    public record ImageAttributionResponse(
            String displayName,
            String uri,
            String photoUri
    ) {
        static ImageAttributionResponse from(final CompanionPostsResult.ImageAttribution attribution) {
            return new ImageAttributionResponse(
                    attribution.displayName(),
                    attribution.uri(),
                    attribution.photoUri()
            );
        }
    }
}
