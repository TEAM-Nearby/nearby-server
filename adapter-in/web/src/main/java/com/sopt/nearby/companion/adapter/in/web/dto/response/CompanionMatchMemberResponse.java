package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchPreview.Member;

public record CompanionMatchMemberResponse(
        Long memberId,
        String profileImageUrl,
        String nickname
) {

    public static CompanionMatchMemberResponse from(Member mmember) {
        return new CompanionMatchMemberResponse(
                mmember.userId(),
                mmember.profileImageUrl(),
                mmember.nickname()
        );
    }
}
