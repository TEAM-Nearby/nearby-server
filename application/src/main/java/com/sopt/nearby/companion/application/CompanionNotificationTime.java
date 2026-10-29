// 동행 알림의 UTC 만남 시각을 장소의 현지 시각으로 변환한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import com.sopt.nearby.companion.domain.model.place.CompanionPlaceCityNameResolver;
import java.time.ZoneOffset;

final class CompanionNotificationTime {
    private CompanionNotificationTime() { }

    static CompanionNotificationSummary localize(final CompanionNotificationSummary summary) {
        var zone = CompanionPlaceCityNameResolver.resolveSupportedCity(summary.placeAddress())
                .map(city -> city.zoneId()).orElse(ZoneOffset.UTC);
        return new CompanionNotificationSummary(summary.notificationId(), summary.applicationId(),
                summary.applicationStatus(), summary.host(), summary.placeName(),
                CompanionMeetingTimeConverter.utcToLocal(summary.meetingAt(), zone), summary.matchId(),
                summary.actionType(), summary.isRead(), summary.placeAddress());
    }
}
