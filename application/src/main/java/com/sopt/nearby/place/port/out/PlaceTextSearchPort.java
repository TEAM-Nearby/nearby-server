// 검색어와 위치 조건으로 외부 장소 검색을 요청하는 포트다.
package com.sopt.nearby.place.port.out;

import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;

public interface PlaceTextSearchPort {
    PlaceSearchPage search(SearchPlacesCommand command);
}
