// 매칭된 동행 목록의 단일 항목 응답을 표현하는 DTO
package com.sopt.nearby.companion.adapter.in.web.dto.response;

import com.sopt.nearby.companion.application.ReadCompanionMatchResult;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchStatus;
import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import com.sopt.nearby.companion.domain.model.post.CompanionPostMeetingTimeType;
import com.sopt.nearby.companion.domain.model.profile.UserGender;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

public record CompanionMatchResponse(
        Long matchId,
        String hostNickname,
        String hostProfileImageUrl,
        UserGender hostGender,
        String placeName,
        CompanionCity city,
        String timeZoneId,
        OffsetDateTime currentLocalTime,
        LocalDateTime meetingAt,
        CompanionPostMeetingTimeType meetingTimeType,
        LocalDateTime createdAt,
        String content,
        CompanionMatchStatus matchStatus
) {

    public static CompanionMatchResponse from(
            final ReadCompanionMatchResult result
    ) {
        final CompanionMatchSummary match = result.match();
        final CompanionCity city = result.city();
        final ZonedDateTime zonedDateTime = result.currentLocalTime();

        final String timeZoneId =
                city == null ? null : city.zoneId().getId();

        final OffsetDateTime currentLocalTime =
                zonedDateTime == null
                        ? null
                        : zonedDateTime.toOffsetDateTime();

        return new CompanionMatchResponse(
                match.matchId(),
                match.hostNickname(),
                match.hostProfileImageUrl(),
                match.hostGender(),
                match.placeName(),
                city,
                timeZoneId,
                currentLocalTime,
                match.meetingAt(),
                match.meetingTimeType(),
                match.createdAt(),
                match.content(),
                match.matchStatus()
        );
    }
}
