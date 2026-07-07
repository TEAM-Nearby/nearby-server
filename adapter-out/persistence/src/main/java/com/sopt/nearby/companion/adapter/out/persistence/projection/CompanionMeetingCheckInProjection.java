// 만남 인증 조회 네이티브 쿼리 결과를 받는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.projection;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionMeetingCheckInProjection {

    Long getMeetingId();

    Long getMatchId();

    String getMeetingStatus();

    Long getScheduleId();

    Long getPlaceId();

    LocalDateTime getScheduledAt();

    BigDecimal getPlaceLatitude();

    BigDecimal getPlaceLongitude();
}
