package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-matches")
@Tag(name = "ComapnionMatch", description = "매칭된 동행 API")
public class CompanionMatchController implements CompanionMatchApi{

    private final ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase;

    public CompanionMatchController(ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase) {
        this.readCompanionMatchPreviewUseCase = readCompanionMatchPreviewUseCase;
    }

    @GetMapping("/{matchId}/preview")
    public CommonResponse<CompanionMatchPreviewResponse> getPreview(
            @PathVariable("matchId") Long matchId
    ) {
        //Todo userId 검증
        CompanionMatchPreview companionMatchPreview = readCompanionMatchPreviewUseCase.getPreview(matchId);


        return CommonResponse.success(CompanionSuccessCode.READ_COMPANION_MATCH_PREVIEW, CompanionMatchPreviewResponse.from(companionMatchPreview));

    }
}
