// 매칭된 동행 목록 조회 응답에 사용되는 매칭 요약 도메인 모델
//DB에서 조회한 매칭·장소 정보

package com.sopt.nearby.companion.domain.model.match;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;

public record CompanionMatchSummary(
        Long matchId,
        String hostNickname,
        String hostProfileImageUrl,
        UserGender hostGender,
        String placeName,
        String placeAddress,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime createdAt,
        String content,
        CompanionMatchStatus matchStatus
) {
}
