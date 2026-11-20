// 매칭된 동행 목록 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionMatchSummaryProjection {
    Long getMatchId();
    String getHostNickname();
    String getHostProfileImageUrl();
    String getHostGender();
    String getPlaceName();
    String getPlaceAddress();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
    LocalDateTime getMeetingAt();
    String getMeetingTimeType();
    LocalDateTime getCreatedAt();
    String getContent();
    String getMatchStatus();
}
