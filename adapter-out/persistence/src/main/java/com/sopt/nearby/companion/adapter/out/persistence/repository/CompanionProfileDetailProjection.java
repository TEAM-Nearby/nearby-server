// 동행 프로필 상세 조회 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface CompanionProfileDetailProjection {

    Long getProfileId();

    Long getUserId();

    String getNickname();

    String getGender();

    Integer getBirthYear();

    String getProfileImageUrl();

    String getIntro();

    BigDecimal getMannerScore();

    Integer getReviewCount();

    String getStatus();

    LocalDateTime getPhoneVerifiedAt();
}
