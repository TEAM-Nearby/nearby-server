// 동행 모집 글 상세 조회 API 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompanionPostDetailResult;
import com.sopt.nearby.companion.domain.model.style.TravelStyleKeyword;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionPostDetailResponse(
        Long postId,
        Long hostUserId,
        Long hostProfileId,
        String googlePlaceId,
        LocalDateTime meetingAt,
        int maxParticipants,
        String content,
        String openChatUrl,
        String status,
        LocalDateTime createdAt,
        String meetingTimeType,
        LocalDateTime expiresAt,
        int participantCount,
        List<ParticipantResponse> participants,
        String applyStatus,
        PlaceResponse place,
        HostProfileSummaryResponse hostProfileSummary
) {

    public static CompanionPostDetailResponse from(final CompanionPostDetailResult result) {
        return new CompanionPostDetailResponse(
                result.postId(),
                result.hostUserId(),
                result.hostProfileId(),
                result.googlePlaceId(),
                result.meetingAt(),
                result.maxParticipants(),
                result.content(),
                result.openChatUrl(),
                result.status().name(),
                result.createdAt(),
                result.meetingTimeType().name(),
                result.expiresAt(),
                result.participantCount(),
                result.participants().stream()
                        .map(ParticipantResponse::from)
                        .toList(),
                result.applyStatus().name(),
                PlaceResponse.from(result.place()),
                HostProfileSummaryResponse.from(result.hostProfileSummary())
        );
    }

    public record ParticipantResponse(
            Long userId,
            String profileImageUrl
    ) {
        static ParticipantResponse from(final CompanionPostDetailResult.Participant participant) {
            return new ParticipantResponse(participant.userId(), participant.profileImageUrl());
        }
    }

    public record PlaceResponse(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category
    ) {
        static PlaceResponse from(final CompanionPostDetailResult.Place place) {
            return new PlaceResponse(
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.latitude(),
                    place.longitude(),
                    place.category().name()
            );
        }
    }

    public record HostProfileSummaryResponse(
            Long profileId,
            String nickname,
            String intro,
            String gender,
            Integer birthYear,
            String profileImageUrl,
            BigDecimal mannerScore,
            LocalDateTime phoneVerifiedAt,
            List<String> keywords
    ) {
        static HostProfileSummaryResponse from(final CompanionPostDetailResult.HostProfileSummary summary) {
            return new HostProfileSummaryResponse(
                    summary.profileId(),
                    summary.nickname(),
                    summary.intro(),
                    summary.gender().name(),
                    summary.birthYear(),
                    summary.profileImageUrl(),
                    summary.mannerScore(),
                    summary.phoneVerifiedAt(),
                    summary.keywords().stream()
                            .map(TravelStyleKeyword::name)
                            .toList()
            );
        }
    }
}
