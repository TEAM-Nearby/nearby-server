package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import java.util.List;

public record CompanionMatchPreviewResponse(
        Long matchId,
        List<CompanionMatchMemberResponse>  members,
        CompanionPostPreviewResponse companionPost
) {

    public static CompanionMatchPreviewResponse from (CompanionMatchPreview preview){
        return new CompanionMatchPreviewResponse(
                preview.matchId(),
                preview.members().stream()
                        .map(CompanionMatchMemberResponse::from)
                        .toList(),
                CompanionPostPreviewResponse.from(preview.companionPost())

        );
    }
}
