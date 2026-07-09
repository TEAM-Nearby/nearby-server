// 매칭된 동행 게시글 미리보기 웹 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.time.LocalDateTime;

public record CompanionPostPreviewResponse(
        Long postId,
        String content,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt
) {

    public static CompanionPostPreviewResponse from(CompanionMatchPreview.Post postModel) {
        return new CompanionPostPreviewResponse(
                postModel.postId(),
                postModel.content(),
                postModel.meetingTimeType(),
                postModel.meetingAt()
        );
    }
}
