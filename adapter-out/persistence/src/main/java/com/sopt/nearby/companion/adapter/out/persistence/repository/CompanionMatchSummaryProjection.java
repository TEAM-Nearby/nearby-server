// 매칭된 동행 목록 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;

public interface CompanionMatchSummaryProjection {
    Long getMatchId();
    String getHostNickname();
    String getPlaceName();
    LocalDateTime getMeetingAt();
    String getContent();
    String getMatchStatus();
}