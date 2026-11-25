// 장소 시간대 조회에 필요한 주소와 좌표를 담는 프로젝션이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

import java.math.BigDecimal;

public interface CompanionPlaceProjection {

    String getAddress();

    BigDecimal getLatitude();

    BigDecimal getLongitude();
}
