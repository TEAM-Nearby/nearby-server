// 동행 모집 글 작성 성공 응답을 표현한다.
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CreateCompanionPostResult;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreatedCompanionPostResponse(
        Long postId,
        String status,
        Long hostUserId,
        PlaceResponse place,
        String meetingTimeType,
        LocalDateTime meetingAt,
        LocalDateTime exposureExpiresAt,
        int maxParticipants,
        int participantCount,
        boolean departEvenIfNotFull,
        List<String> styleKeywords,
        String content,
        String openChatUrl,
        LocalDateTime createdAt
) {

    public static CreatedCompanionPostResponse from(final CreateCompanionPostResult result) {
        return new CreatedCompanionPostResponse(
                result.postId(),
                result.status().name(),
                result.hostUserId(),
                PlaceResponse.from(result.place()),
                result.meetingTimeType().name(),
                result.meetingAt(),
                result.exposureExpiresAt(),
                result.maxParticipants(),
                result.participantCount(),
                result.departEvenIfNotFull(),
                result.styleKeywords().stream()
                        .map(Enum::name)
                        .toList(),
                result.content(),
                result.openChatUrl(),
                result.createdAt()
        );
    }

    public record PlaceResponse(
            Long placeId,
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category
    ) {

        static PlaceResponse from(final CreateCompanionPostResult.Place place) {
            return new PlaceResponse(
                    place.placeId(),
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.latitude(),
                    place.longitude(),
                    place.category().name()
            );
        }
    }
}
