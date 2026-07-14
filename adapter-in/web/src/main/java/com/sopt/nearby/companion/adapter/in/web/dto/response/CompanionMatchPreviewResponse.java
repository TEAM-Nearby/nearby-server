// 매칭된 동행 미리보기 웹 응답 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CompanionMatchPreviewResponse(
        Long matchId,
        @Schema(description = "모집글 호스트 정보", requiredMode = Schema.RequiredMode.REQUIRED)
        HostResponse host,
        @Schema(description = "호스트를 제외한 매칭 참여자 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<CompanionMatchMemberResponse> members,
        CompanionPostPreviewResponse companionPost
) {

    public static CompanionMatchPreviewResponse from(CompanionMatchPreview preview) {
        return new CompanionMatchPreviewResponse(
                preview.matchId(),
                HostResponse.from(preview.host()),
                preview.members().stream()
                        .map(CompanionMatchMemberResponse::from)
                        .toList(),
                CompanionPostPreviewResponse.from(preview.companionPost())

        );
    }

    public record HostResponse(
            @Schema(description = "호스트 닉네임", example = "조로")
            String hostName,
            @Schema(
                    description = "호스트 프로필 이미지 URL",
                    example = "https://image.url/hostProfile.png",
                    nullable = true
            )
            String hostProfileImageUrl
    ) {

        private static HostResponse from(final CompanionMatchPreview.Member host) {
            return new HostResponse(host.nickname(), host.profileImageUrl());
        }
    }
}
