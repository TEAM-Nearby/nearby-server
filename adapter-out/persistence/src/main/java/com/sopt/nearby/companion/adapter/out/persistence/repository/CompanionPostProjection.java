// 동행 모집글 목록 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionPostProjection {

    Long getPostId();

    String getStatus();

    String getHostNickname();

    String getHostGender();

    Long getPlaceId();

    String getGooglePlaceId();

    String getPlaceName();

    String getPlaceCategory();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    Number getDistanceMeters();

    String getPhotoReference();

    String getContent();

    LocalDateTime getMeetingAt();

    Number getParticipantCount();

    Number getMaxParticipants();

    LocalDateTime getCreatedAt();
}
