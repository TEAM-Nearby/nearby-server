// 동행 매치 목록 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import java.util.List;

public record CompanionMatchesResponse(
        List<CompanionMatchResponse> matches
) {
    public static CompanionMatchesResponse from(final List<CompanionMatchSummary> summaries) {
        return new CompanionMatchesResponse(
                summaries.stream()
                        .map(CompanionMatchResponse::from)
                        .toList()
        );
    }
}