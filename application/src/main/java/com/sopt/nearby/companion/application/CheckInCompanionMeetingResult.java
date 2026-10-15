// 동행 만남 인증 결과를 표현하는 응답 모델
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.meeting.CompanionMeetingStatus;
import java.time.LocalDateTime;

public record CheckInCompanionMeetingResult(
        Long meetingId,
        CompanionMeetingStatus meetingStatus,
        boolean currentUserCheckedIn,
        long checkedInCount,
        long totalParticipantCount,
        boolean allParticipantsCheckedIn,
        boolean canMoveToComplete,
        LocalDateTime checkedInAt,
        double distanceMeters,
        double allowedRadiusMeters,
        LocalDateTime checkInAvailableFrom,
        LocalDateTime checkInAvailableUntil,
        boolean alreadyCompleted
) {
}
