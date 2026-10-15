// 매칭된 동행 게시글 미리보기 웹 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

public record CompanionPostPreviewResponse(
        Long postId,
        String content,
        @Schema(description = "만남 장소명", example = "바르셀로나 고딕 지구", requiredMode = Schema.RequiredMode.REQUIRED)
        String placeName,
        CompanionCity city,
        String timeZoneId,
        OffsetDateTime currentLocalTime,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime meetingAt
) {

    public static CompanionPostPreviewResponse from(CompanionMatchPreview.Post postModel) {
        return new CompanionPostPreviewResponse(
                postModel.postId(),
                postModel.content(),
                postModel.placeName(),
                postModel.city(),
                postModel.city() == null ? null : postModel.city().zoneId().getId(),
                postModel.currentLocalTime() == null ? null : postModel.currentLocalTime().toOffsetDateTime(),
                postModel.meetingTimeType(),
                postModel.meetingAt()
        );
    }
}
