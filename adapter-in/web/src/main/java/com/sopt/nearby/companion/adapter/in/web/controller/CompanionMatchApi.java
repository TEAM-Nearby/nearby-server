package com.sopt.nearby.companion.adapter.in.web.controller;


import com.sopt.nearby.companion.adapter.in.web.dto.response.CompanionMatchPreviewResponse;
import com.sopt.nearby.companion.domain.exception.CompanionMatchNotFoundException;
import com.sopt.nearby.companion.domain.exception.ForbiddenCompanionMatchException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.PathVariable;


@Tag(name = "ComapnionMatch", description = "매칭된 동행 API")
public interface CompanionMatchApi {

    @ApiExceptions({
            CompanionMatchNotFoundException.class,
            ForbiddenCompanionMatchException.class
    })
    @Operation(summary = "매칭된 동행 미리보기", description = "매칭된 동행 미리보기를 조회합니다.")
    CommonResponse<CompanionMatchPreviewResponse> getPreview(
            @PathVariable Long matchId
    );
}
