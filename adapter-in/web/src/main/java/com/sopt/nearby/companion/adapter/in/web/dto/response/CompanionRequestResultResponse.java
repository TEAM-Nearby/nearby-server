// 신청자에게 동행 신청 상태와 수락 상세를 전달하는 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CompanionRequestResult;
import com.sopt.nearby.companion.domain.model.match.AcceptedCompanionRequestDetail;
import com.sopt.nearby.companion.domain.model.match.CompanionApplicationStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionRequestResultResponse(
        Long applicationId,
        CompanionApplicationStatus applicationStatus,
        AcceptedResult acceptedResult
) {

    public static CompanionRequestResultResponse from(final CompanionRequestResult result) {
        return new CompanionRequestResultResponse(
                result.applicationId(),
                result.applicationStatus(),
                result.acceptedDetail() == null ? null : AcceptedResult.from(result.acceptedDetail())
        );
    }

    public record AcceptedResult(
            Long matchId,
            CompanionMatchStatus matchStatus,
            Long postId,
            Host host,
            Place place,
            CompanionPostMeetingTimeType meetingTimeType,
            LocalDateTime meetingAt,
            int participantCount,
            int maxParticipants,
            String openChatUrl
    ) {

        private static AcceptedResult from(final AcceptedCompanionRequestDetail detail) {
            return new AcceptedResult(
                    detail.matchId(),
                    detail.matchStatus(),
                    detail.postId(),
                    Host.from(detail.host()),
                    Place.from(detail.place()),
                    detail.meetingTimeType(),
                    detail.meetingAt(),
                    detail.participantCount(),
                    detail.maxParticipants(),
                    detail.openChatUrl()
            );
        }
    }

    public record Host(
            Long userId,
            String nickname,
            String profileImageUrl
    ) {

        private static Host from(final AcceptedCompanionRequestDetail.Host host) {
            return new Host(host.userId(), host.nickname(), host.profileImageUrl());
        }
    }

    public record Place(
            String googlePlaceId,
            String name,
            String address,
            BigDecimal latitude,
            BigDecimal longitude
    ) {

        private static Place from(final AcceptedCompanionRequestDetail.Place place) {
            return new Place(
                    place.googlePlaceId(),
                    place.name(),
                    place.address(),
                    place.latitude(),
                    place.longitude()
            );
        }
    }
}
