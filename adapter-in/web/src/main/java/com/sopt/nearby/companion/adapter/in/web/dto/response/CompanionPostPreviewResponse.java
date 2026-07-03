package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;

public record CompanionPostPreviewResponse(
        Long postId,
        String content
) {

    public static CompanionPostPreviewResponse from(CompanionMatchPreview.Post postModel) {
        return new CompanionPostPreviewResponse(postModel.postId(), postModel.content());
    }
}
