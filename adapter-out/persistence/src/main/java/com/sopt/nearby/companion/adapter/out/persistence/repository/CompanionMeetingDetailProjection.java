// 동행 상세 조회 쿼리 결과를 담는 Projection
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.time.LocalDateTime;

public interface CompanionMeetingDetailProjection {

    Long getMeetingId();

    String getCurrentUserRole();

    Long getHostId();

    String getHostGender();

    String getHostProfileImageUrl();

    String getHostNickname();

    Boolean getHostCheckedIn();

    String getPlaceName();

    LocalDateTime getMeetingAt();

    String getMeetingStatus();

    Boolean getCurrentUserCheckedIn();
}
