// 혼밥 맛집 목록 네이티브 쿼리 결과를 받는 Projection이다.
package com.sopt.nearby.place.adapter.out.persistence.repository;

import java.math.BigDecimal;

public interface SoloDiningPlaceProjection {

    Long getPlaceId();

    String getGooglePlaceId();

    String getName();

    String getAddress();

    String getPhotoReference();

    String getCategory();

    Number getDistanceMeters();

    BigDecimal getRating();

    Integer getReviewCount();

    Boolean getFavorite();

    BigDecimal getLatitude();

    BigDecimal getLongitude();

    String getBusinessStatus();
}
