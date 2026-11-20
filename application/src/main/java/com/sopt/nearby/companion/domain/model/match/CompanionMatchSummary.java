// 매칭된 동행 목록 조회 응답에 사용되는 매칭 요약 도메인 모델
//DB에서 조회한 매칭·장소 정보

package com.sopt.nearby.companion.domain.model.match;

import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CompanionMatchSummary(
        Long matchId,
        String hostNickname,
        String hostProfileImageUrl,
        UserGender hostGender,
        String placeName,
        String placeAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime createdAt,
        String content,
        CompanionMatchStatus matchStatus
) {

    public CompanionMatchSummary(
            final Long matchId,
            final String hostNickname,
            final String hostProfileImageUrl,
            final UserGender hostGender,
            final String placeName,
            final String placeAddress,
            final LocalDateTime meetingAt,
            final CompanionPostMeetingTimeType meetingTimeType,
            final LocalDateTime createdAt,
            final String content,
            final CompanionMatchStatus matchStatus
    ) {
        this(matchId, hostNickname, hostProfileImageUrl, hostGender, placeName, placeAddress, null, null,
                meetingAt, meetingTimeType, createdAt, content, matchStatus);
    }
}
