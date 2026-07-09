// 마이페이지 조회 프로필 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface MyPageProfileProjection {

    Long getProfileId();

    Long getUserId();

    String getNickname();

    String getGender();

    Integer getBirthYear();

    String getProfileImageUrl();

    BigDecimal getMannerScore();

    Integer getReviewCount();

    LocalDateTime getPhoneVerifiedAt();
}
