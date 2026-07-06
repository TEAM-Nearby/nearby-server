// 동행 알림 목록 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;

public interface CompanionNotificationProjection {

    Long getApplicationId();

    String getApplicationStatus();

    Long getHostUserId();

    String getHostProfileImageUrl();

    String getHostNickname();

    String getPlaceName();

    LocalDateTime getMeetingAt();

    Long getMatchId();

    Boolean getReadStatus();
}

