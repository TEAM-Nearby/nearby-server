// 동행 모집 글 상세 조회 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionPostDetailProjection {

    Long getPostId();

    Long getHostUserId();

    Long getHostProfileId();

    String getHostNickname();

    String getHostIntro();

    String getHostGender();

    Integer getHostBirthYear();

    String getHostProfileImageUrl();

    BigDecimal getHostMannerScore();

    LocalDateTime getHostPhoneVerifiedAt();

    String getGooglePlaceId();

    String getPlaceName();

    String getPlaceAddress();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    String getPlaceCategory();

    LocalDateTime getMeetingAt();

    Number getMaxParticipants();

    String getContent();

    String getOpenChatUrl();

    String getStatus();

    LocalDateTime getCreatedAt();

    String getMeetingTimeType();

    LocalDateTime getExpiresAt();

    Number getParticipantCount();

    String getApplicationStatus();
}
