// 동행 매칭 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-matches")
@Tag(name = "CompanionMatch", description = "매칭된 동행 API")
public class CompanionMatchController implements CompanionMatchApi {

    private final ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase;

    public CompanionMatchController(final ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase) {
        this.readCompanionMatchPreviewUseCase = readCompanionMatchPreviewUseCase;
    }

    @Override
    @GetMapping("/{matchId}/preview")
    public CommonResponse<CompanionMatchPreviewResponse> getPreview(
            @PathVariable("matchId") final Long matchId,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CompanionMatchPreview companionMatchPreview = readCompanionMatchPreviewUseCase.getPreview(matchId, userId);

        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_MATCH_PREVIEW,
                CompanionMatchPreviewResponse.from(companionMatchPreview)
        );

    }
}
