// 외부 장소 검색 포트를 정의한다.
package com.sopt.nearby.place.port.out;

import java.util.List;

public interface SoloDiningPlaceSearchPort {

    List<SoloDiningPlaceSearchResult> search(SoloDiningPlaceSearchRequest request);
}
