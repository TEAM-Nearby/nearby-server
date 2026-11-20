// 동행 알림의 UTC 만남 시각을 장소의 현지 시각으로 변환한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.notification.CompanionNotificationSummary;
import java.time.Instant;

final class CompanionNotificationTime {
    private CompanionNotificationTime() { }

    static CompanionNotificationSummary localize(final CompanionNotificationSummary summary) {
        return localize(summary, new CompanionPlaceTimeResolver(), Instant.EPOCH);
    }

    static CompanionNotificationSummary localize(
            final CompanionNotificationSummary summary,
            final CompanionPlaceTimeResolver placeTimeResolver,
            final Instant instant
    ) {
        var zone = placeTimeResolver.resolveZone(
                summary.placeAddress(), summary.latitude(), summary.longitude(), instant);
        return new CompanionNotificationSummary(summary.notificationId(), summary.applicationId(),
                summary.applicationStatus(), summary.host(), summary.placeName(),
                CompanionMeetingTimeConverter.utcToLocal(summary.meetingAt(), zone), summary.matchId(),
                summary.actionType(), summary.isRead(), summary.placeAddress(),
                summary.latitude(), summary.longitude());
    }
}
