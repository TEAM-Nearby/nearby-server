// 혼밥 맛집 즐겨찾기 목록 네이티브 쿼리 결과를 받는 Projection이다.
package com.sopt.nearby.place.adapter.out.persistence.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface SoloDiningFavoriteProjection {

    Long getFavoriteId();

    LocalDateTime getCreatedAt();

    Long getPlaceId();

    String getGooglePlaceId();

    String getName();

    String getAddress();

    String getPhotoReference();

    String getCategory();

    Number getDistanceMeters();

    BigDecimal getRating();

    Integer getReviewCount();

    String getBusinessStatus();
}
