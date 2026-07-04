// 동행 매칭 컨트롤러
package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.code.CompanionSuccessCode;
import com.sopt.nearby.companion.adapter.in.web.dto.request.CompanionMatchScheduleRequest;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchScheduleResponse;
import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchesResponse;
import com.sopt.nearby.companion.application.ConfirmCompanionScheduleResult;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.port.in.ConfirmCompanionScheduleUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchPreviewUseCase;
import com.sopt.nearby.companion.port.in.ReadCompanionMatchesUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import org.springframework.web.bind.annotation.RequestBody;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-matches")
public class CompanionMatchController implements CompanionMatchApi {

    private final ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase;
    private final ReadCompanionMatchesUseCase readCompanionMatchesUseCase;
    private final ConfirmCompanionScheduleUseCase confirmCompanionScheduleUseCase;

    public CompanionMatchController(final ReadCompanionMatchPreviewUseCase readCompanionMatchPreviewUseCase,
                                    ReadCompanionMatchesUseCase readCompanionMatchesUseCase,
                                    ConfirmCompanionScheduleUseCase confirmCompanionScheduleUseCase) {
        this.readCompanionMatchPreviewUseCase = readCompanionMatchPreviewUseCase;
        this.readCompanionMatchesUseCase = readCompanionMatchesUseCase;
        this.confirmCompanionScheduleUseCase = confirmCompanionScheduleUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<CompanionMatchesResponse> getMatches(
            final Principal principal
    ){
        Long userId = Long.valueOf(principal.getName());
        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_MATCHES,
                CompanionMatchesResponse.from(readCompanionMatchesUseCase.getMatches(userId))
        );
    }

    @Override
    @GetMapping("/{matchId}/preview")
    public CommonResponse<CompanionMatchPreviewResponse> getPreview(
            @PathVariable final Long matchId,
            final Principal principal
    ) {
        Long userId = Long.valueOf(principal.getName());
        CompanionMatchPreview companionMatchPreview = readCompanionMatchPreviewUseCase.getPreview(matchId, userId);

        return CommonResponse.success(
                CompanionSuccessCode.READ_COMPANION_MATCH_PREVIEW,
                CompanionMatchPreviewResponse.from(companionMatchPreview)
        );

    }

    @Override
    @PostMapping("/{matchId}/schedule")
    public CommonResponse<CompanionMatchScheduleResponse> postSchedule(
            @PathVariable final Long matchId,
            @RequestBody final CompanionMatchScheduleRequest request,
            final Principal principal
    ){
        Long userId = Long.valueOf(principal.getName());

        ConfirmCompanionScheduleResult result = confirmCompanionScheduleUseCase.confirm(
                request.toCommand(matchId, userId)
        );
        return CommonResponse.success(
                CompanionSuccessCode.CONFIRM_COMPANION_SCHEDULE,
                CompanionMatchScheduleResponse.from(result)
        );
    }}
