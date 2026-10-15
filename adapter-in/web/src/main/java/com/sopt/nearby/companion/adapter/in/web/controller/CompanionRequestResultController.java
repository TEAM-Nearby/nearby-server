// 신청자의 동행 신청 결과 조회 요청을 유스케이스로 전달한다.
package com.sopt.nearby.companion.adapter.in.web.controller;

import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionRequestResultResponse;
import com.sopt.nearby.companion.application.ReadCompanionRequestResultCommand;
import com.sopt.nearby.companion.port.in.ReadCompanionRequestResultUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/companion-requests")
public class CompanionRequestResultController implements CompanionRequestResultApi {

    private final ReadCompanionRequestResultUseCase useCase;

    public CompanionRequestResultController(final ReadCompanionRequestResultUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    @GetMapping("/{applicationId}/result")
    public CommonResponse<CompanionRequestResultResponse> getResult(
            @PathVariable final Long applicationId,
            final Principal principal
    ) {
        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_REQUEST_RESULT,
                CompanionRequestResultResponse.from(useCase.read(
                        new ReadCompanionRequestResultCommand(
                                Long.valueOf(principal.getName()),
                                applicationId
                        )
                ))
        );
    }
}
