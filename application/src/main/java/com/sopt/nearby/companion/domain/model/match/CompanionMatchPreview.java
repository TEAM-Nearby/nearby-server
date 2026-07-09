// 동행 매칭 미리보기 응답용 도메인 모델
package com.sopt.nearby.companion.domain.model.match;


import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import java.time.LocalDateTime;
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
            String content,
            CompanionPostMeetingTimeType meetingTimeType,
            LocalDateTime meetingAt
    ) {

    }
}
