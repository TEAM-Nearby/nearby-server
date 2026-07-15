// 동행 신청 검토 화면 상세 조회 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionRequestReviewProjection {

    Long getApplicationId();

    Long getPostId();

    String getApplicationStatus();

    Long getHostUserId();

    String getPlaceName();

    String getMeetingTimeType();

    LocalDateTime getMeetingAt();

    LocalDateTime getExposureExpiresAt();

    Long getApplicantProfileId();

    String getApplicantProfileImageUrl();

    String getApplicantNickname();

    String getApplicantGender();

    Integer getApplicantBirthYear();

    BigDecimal getApplicantMannerScore();

    LocalDateTime getApplicantPhoneVerifiedAt();
}
