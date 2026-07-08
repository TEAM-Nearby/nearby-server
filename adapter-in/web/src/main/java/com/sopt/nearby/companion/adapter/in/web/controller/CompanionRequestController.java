// 동행 신청 검토 화면 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionRequestReviewResponse;
import com.sopt.nearby.companion.application.ReadCompanionRequestReviewCommand;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestReviewUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-requests")
public class CompanionRequestController implements CompanionRequestApi {

    private final ReadCompanionRequestReviewUseCase readCompanionRequestReviewUseCase;

    public CompanionRequestController(final ReadCompanionRequestReviewUseCase readCompanionRequestReviewUseCase) {
        this.readCompanionRequestReviewUseCase = readCompanionRequestReviewUseCase;
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
}
