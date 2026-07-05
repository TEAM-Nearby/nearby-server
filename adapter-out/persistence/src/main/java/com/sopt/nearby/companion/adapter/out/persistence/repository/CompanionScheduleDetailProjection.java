// 내 동행 일정 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionScheduleDetailProjection {
    Long getMatchId();
    String getMatchStatus();
    Long getScheduleId();
    String getGooglePlaceId();
    String getPlaceName();
    String getPlaceAddress();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    LocalDateTime getScheduledAt();
    String getOpenChatUrl();
}