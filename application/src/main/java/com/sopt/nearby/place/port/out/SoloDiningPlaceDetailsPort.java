// 외부 장소 상세 조회 포트를 정의한다.
package com.sopt.nearby.place.port.out;

public interface SoloDiningPlaceDetailsPort {

    SoloDiningPlaceDetailsResult findByGooglePlaceId(String googlePlaceId);
}
