// 진행 중인 동행 상세 조회 응답 본문을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadCompanionMeetingDetailResult;
import java.time.LocalDateTime;

public record CompanionMeetingDetailResponse(
        Long meetingId,
        String currentUserRole,
        Long hostId,
        String hostGender,
        String hostProfileImageUrl,
        String hostNickname,
        boolean hostCheckedIn,
        String placeName,
        LocalDateTime meetingAt,
        String meetingStatus,
        boolean currentUserCheckedIn,
        boolean canCancelMeeting
) {

    public static CompanionMeetingDetailResponse from(final ReadCompanionMeetingDetailResult result) {
        return new CompanionMeetingDetailResponse(
                result.meetingId(),
                result.currentUserRole().name(),
                result.hostId(),
                result.hostGender().name(),
                result.hostProfileImageUrl(),
                result.hostNickname(),
                result.hostCheckedIn(),
                result.placeName(),
                result.meetingAt(),
                result.meetingStatus().name(),
                result.currentUserCheckedIn(),
                result.canCancelMeeting()
        );
    }
}
