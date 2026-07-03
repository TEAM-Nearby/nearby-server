package com.sopt.nearby.companion.domain.model.match;


import java.util.List;

public record CompanionMatchPreview(
        Long matchId,
        List<Member> members,
        Post companionPost
) {

    public record Member(
            Long userId,
            String profileImageUrl,
            String nickname
    ) {
    }
    public record Post(
            Long postId,
            String content
    ){

    }
}
