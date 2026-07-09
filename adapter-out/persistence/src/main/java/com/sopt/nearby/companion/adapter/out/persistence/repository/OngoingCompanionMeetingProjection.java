// 진행 중인 동행 목록 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;

public interface OngoingCompanionMeetingProjection {

    Long getMeetingId();

    Long getMatchId();

    Long getHostUserId();

    String getHostProfileImageUrl();

    String getHostNickname();

    String getHostGender();

    String getPlaceName();

    LocalDateTime getMeetingAt();

    String getMeetingTimeType();

    Boolean getCheckedIn();

    String getMeetingStatus();
}
