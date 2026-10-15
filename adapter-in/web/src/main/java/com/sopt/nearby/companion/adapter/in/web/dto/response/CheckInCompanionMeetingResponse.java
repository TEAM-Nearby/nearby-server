// 동행 만남 인증 응답 본문을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;
import java.time.LocalDateTime;

public record CheckInCompanionMeetingResponse(
        Long meetingId,
        String meetingStatus,
        boolean currentUserCheckedIn,
        long checkedInCount,
        long totalParticipantCount,
        boolean allParticipantsCheckedIn,
        boolean canMoveToComplete,
        LocalDateTime checkedInAt,
        double distanceMeters,
        double allowedRadiusMeters,
        LocalDateTime checkInAvailableFrom,
        LocalDateTime checkInAvailableUntil
) {

    public static CheckInCompanionMeetingResponse from(final CheckInCompanionMeetingResult result) {
        return new CheckInCompanionMeetingResponse(
                result.meetingId(),
                result.meetingStatus().name(),
                result.currentUserCheckedIn(),
                result.checkedInCount(),
                result.totalParticipantCount(),
                result.allParticipantsCheckedIn(),
                result.canMoveToComplete(),
                result.checkedInAt(),
                result.distanceMeters(),
                result.allowedRadiusMeters(),
                result.checkInAvailableFrom(),
                result.checkInAvailableUntil()
        );
    }
}
