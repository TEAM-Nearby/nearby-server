// 동행 신청 검토 화면 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.RejectCompanionRequestRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.AcceptedCompanionRequestResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionRequestReviewResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.RejectedCompanionRequestResponse;
import com.sopt.nearby.companion.application.AcceptCompanionRequestCommand;
import com.sopt.nearby.companion.application.ReadCompanionRequestReviewCommand;
import com.sopt.nearby.companion.application.RejectCompanionRequestCommand;
import com.sopt.nearby.companion.port.in.AcceptCompanionRequestUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestReviewUseCase;
import com.sopt.nearby.companion.port.in.RejectCompanionRequestUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-requests")
public class CompanionRequestController implements CompanionRequestApi {

    private final ReadCompanionRequestReviewUseCase readCompanionRequestReviewUseCase;
    private final AcceptCompanionRequestUseCase acceptCompanionRequestUseCase;
    private final RejectCompanionRequestUseCase rejectCompanionRequestUseCase;

    public CompanionRequestController(
            final ReadCompanionRequestReviewUseCase readCompanionRequestReviewUseCase,
            final AcceptCompanionRequestUseCase acceptCompanionRequestUseCase,
            final RejectCompanionRequestUseCase rejectCompanionRequestUseCase
    ) {
        this.readCompanionRequestReviewUseCase = readCompanionRequestReviewUseCase;
        this.acceptCompanionRequestUseCase = acceptCompanionRequestUseCase;
        this.rejectCompanionRequestUseCase = rejectCompanionRequestUseCase;
    }

    @Override
    @GetMapping("/{applicationId}/review")
    public CommonResponse<CompanionRequestReviewResponse> getReview(
            @PathVariable final Long applicationId,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_REQUEST_REVIEW,
                CompanionRequestReviewResponse.from(readCompanionRequestReviewUseCase.read(
                        new ReadCompanionRequestReviewCommand(
                                Long.valueOf(principal.getName()),
                                applicationId
                        )
                ))
        );
    }

    @Override
    @PatchMapping("/{applicationId}/accept")
    public CommonResponse<AcceptedCompanionRequestResponse> accept(
            @PathVariable final Long applicationId,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.ACCEPT_COMPANION_REQUEST,
                AcceptedCompanionRequestResponse.from(acceptCompanionRequestUseCase.accept(
                        new AcceptCompanionRequestCommand(
                                Long.valueOf(principal.getName()),
                                applicationId
                        )
                ))
        );
    }

    @Override
    @PatchMapping("/{applicationId}/reject")
    public CommonResponse<RejectedCompanionRequestResponse> reject(
            @PathVariable final Long applicationId,
            @RequestBody(required = false) final RejectCompanionRequestRequest request,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.REJECT_COMPANION_REQUEST,
                RejectedCompanionRequestResponse.from(rejectCompanionRequestUseCase.reject(
                        new RejectCompanionRequestCommand(
                                Long.valueOf(principal.getName()),
                                applicationId,
                                request == null ? null : request.rejectionReason()
                        )
                ))
        );
    }
}
