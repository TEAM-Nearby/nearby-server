// 동행 매치 목록 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadCompanionMatchResult;
import java.util.List;

public record CompanionMatchesResponse(
        List<CompanionMatchResponse> matches
) {
    public static CompanionMatchesResponse from(final List<ReadCompanionMatchResult> results) {
        return new CompanionMatchesResponse(
                results.stream()
                        .map(CompanionMatchResponse::from)
                        .toList()
        );
    }
}
