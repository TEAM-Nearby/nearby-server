// 동행 매칭 조회 결과와 장소 현지 시각을 담는다.
// Summary + 계산된 도시 + 현지 시각
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.match.CompanionMatchSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

public record ReadCompanionMatchResult(
        CompanionMatchSummary match,
        CompanionCity city,
        ZonedDateTime currentLocalTime,
        LocalDateTime meetingAt
) {

    public LocalDateTime localCreatedAt() {
        return CompanionMeetingTimeConverter.utcToLocal(match.createdAt(),
                city == null ? java.time.ZoneOffset.UTC : city.zoneId());
    }

    public ReadCompanionMatchResult(
            final CompanionMatchSummary match,
            final CompanionCity city,
            final ZonedDateTime currentLocalTime
    ) {
        this(match, city, currentLocalTime, match.meetingAt());
    }
}
