// 동행 만남 인증 HTTP 요청을 처리하는 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CheckInCompanionMeetingRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CheckInCompanionMeetingResponse;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-meetings")
public class CompanionMeetingController implements CompanionMeetingApi {

    private final CheckInCompanionMeetingUseCase checkInCompanionMeetingUseCase;

    public CompanionMeetingController(final CheckInCompanionMeetingUseCase checkInCompanionMeetingUseCase) {
        this.checkInCompanionMeetingUseCase = checkInCompanionMeetingUseCase;
    }

    @Override
    @PostMapping("/{meetingId}/check-in")
    public CommonResponse<CheckInCompanionMeetingResponse> checkIn(
            @PathVariable final Long meetingId,
            @RequestBody final CheckInCompanionMeetingRequest request,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CheckInCompanionMeetingResult result = checkInCompanionMeetingUseCase.checkIn(
                request.toCommand(meetingId, userId)
        );

        return CommonResponse.success(
                successCode(result),
                CheckInCompanionMeetingResponse.from(result)
        );
    }

    private CompanionSuccessCode successCode(final CheckInCompanionMeetingResult result) {
        if (result.alreadyCompleted()) {
            return CompanionSuccessCode.CHECK_IN_COMPANION_MEETING_ALREADY_COMPLETED;
        }
        return CompanionSuccessCode.CHECK_IN_COMPANION_MEETING;
    }
}
