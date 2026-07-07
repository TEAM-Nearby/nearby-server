// 진행 중인 동행 목록 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import java.util.List;

public record OngoingCompanionMeetingsResponse(
        List<OngoingCompanionMeetingResponse> meetings
) {

    public static OngoingCompanionMeetingsResponse from(final List<OngoingCompanionMeetingSummary> summaries) {
        return new OngoingCompanionMeetingsResponse(
                summaries.stream()
                        .map(OngoingCompanionMeetingResponse::from)
                        .toList()
        );
    }
}
