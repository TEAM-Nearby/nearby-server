// 동행 시각을 UTC와 장소 현지 시각 사이에서 변환한다.
package com.sopt.nearby.companion.application;

import com.sopt.nearby.companion.domain.model.place.CompanionCity;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

final class CompanionMeetingTimeConverter {

    private CompanionMeetingTimeConverter() {
    }

    static Optional<LocalDateTime> toUtc(final LocalDateTime localTime, final ZoneId zone) {
        if (localTime == null || zone == null) {
            return Optional.empty();
        }
        List<ZoneOffset> offsets = zone.getRules().getValidOffsets(localTime);
        if (offsets.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(localTime.atOffset(offsets.getFirst())
                .withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    static LocalDateTime utcToLocal(final LocalDateTime utcTime, final ZoneId zone) {
        if (utcTime == null || zone == null) {
            return null;
        }
        return utcTime.atOffset(ZoneOffset.UTC).atZoneSameInstant(zone).toLocalDateTime();
    }

    static LocalDateTime toPlaceLocalTime(
            final LocalDateTime meetingAt,
            final CompanionCity city
    ) {
        return utcToLocal(meetingAt, city == null ? ZoneOffset.UTC : city.zoneId());
    }
}
