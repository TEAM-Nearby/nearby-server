// 마이페이지 완료 동행 장소 네이티브 쿼리 결과를 받는 projection이다.
package com.sopt.nearby.companion.adapter.out.persistence.repository;

public interface MyPageVisitedPlaceProjection {

    Long getMeetingId();

    String getPlaceName();

    String getPlaceAddress();
}
