// 동행 만남 인증 HTTP 요청을 처리하는 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CheckInCompanionMeetingRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CreateCompanionReviewsRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CheckInCompanionMeetingResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMeetingDetailResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CreateCompanionReviewsResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.OngoingCompanionMeetingsResponse;
import com.sopt.nearby.companion.application.CheckInCompanionMeetingResult;
import com.sopt.nearby.companion.application.CreateCompanionReviewsResult;
import com.sopt.nearby.companion.application.ReadCompanionMeetingDetailResult;
import com.sopt.nearby.companion.domain.model.meeting.OngoingCompanionMeetingSummary;
import com.sopt.nearby.companion.port.in.CheckInCompanionMeetingUseCase;
import com.sopt.nearby.companion.port.in.CreateCompanionReviewsUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMeetingDetailUseCase;
import com.sopt.nearby.companion.port.in.ReadOngoingCompanionMeetingsUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-meetings")
public class CompanionMeetingController implements CompanionMeetingApi {

    private final ReadOngoingCompanionMeetingsUseCase readOngoingCompanionMeetingsUseCase;
    private final ReadCompanionMeetingDetailUseCase readCompanionMeetingDetailUseCase;
    private final CheckInCompanionMeetingUseCase checkInCompanionMeetingUseCase;
    private final CreateCompanionReviewsUseCase createCompanionReviewsUseCase;

    public CompanionMeetingController(
            final ReadOngoingCompanionMeetingsUseCase readOngoingCompanionMeetingsUseCase,
            final ReadCompanionMeetingDetailUseCase readCompanionMeetingDetailUseCase,
            final CheckInCompanionMeetingUseCase checkInCompanionMeetingUseCase,
            final CreateCompanionReviewsUseCase createCompanionReviewsUseCase
    ) {
        this.readOngoingCompanionMeetingsUseCase = readOngoingCompanionMeetingsUseCase;
        this.readCompanionMeetingDetailUseCase = readCompanionMeetingDetailUseCase;
        this.checkInCompanionMeetingUseCase = checkInCompanionMeetingUseCase;
        this.createCompanionReviewsUseCase = createCompanionReviewsUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<OngoingCompanionMeetingsResponse> getOngoingMeetings(final Principal principal) {
        Long userId = Long.valueOf(principal.getName());
        List<OngoingCompanionMeetingSummary> meetings = readOngoingCompanionMeetingsUseCase.getOngoingMeetings(userId);

        return CommonResponse.success(
                CompanionSuccessCode.READ_ONGOING_COMPANION_MEETINGS,
                OngoingCompanionMeetingsResponse.from(meetings)
        );
    }

    @Override
    @GetMapping("/{meetingId}")
    public CommonResponse<CompanionMeetingDetailResponse> getDetail(
            @PathVariable final Long meetingId,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        ReadCompanionMeetingDetailResult result = readCompanionMeetingDetailUseCase.getDetail(meetingId, userId);

        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_MEETING_DETAIL,
                CompanionMeetingDetailResponse.from(result)
        );
    }

    @Override
    @PostMapping("/{meetingId}/check-in")
    public CommonResponse<CheckInCompanionMeetingResponse> checkIn(
            @PathVariable final Long meetingId,
            @RequestBody(required = false) final CheckInCompanionMeetingRequest request,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CheckInCompanionMeetingResult result = checkInCompanionMeetingUseCase.checkIn(
                request == null ? null : request.toCommand(meetingId, userId)
        );

        return CommonResponse.success(
                successCode(result),
                CheckInCompanionMeetingResponse.from(result)
        );
    }

    @Override
    @PostMapping("/{meetingId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public CommonResponse<CreateCompanionReviewsResponse> createReviews(
            @PathVariable final Long meetingId,
            @RequestBody(required = false) final CreateCompanionReviewsRequest request,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CreateCompanionReviewsResult result = createCompanionReviewsUseCase.create(
                request == null ? null : request.toCommand(meetingId, userId)
        );

        return CommonResponse.created(
                CompanionSuccessCode.CREATE_COMPANION_REVIEWS,
                CreateCompanionReviewsResponse.from(result)
        );
    }

    private CompanionSuccessCode successCode(final CheckInCompanionMeetingResult result) {
        if (result.alreadyCompleted()) {
            return CompanionSuccessCode.CHECK_IN_COMPANION_MEETING_ALREADY_COMPLETED;
        }
        return CompanionSuccessCode.CHECK_IN_COMPANION_MEETING;
    }
}
