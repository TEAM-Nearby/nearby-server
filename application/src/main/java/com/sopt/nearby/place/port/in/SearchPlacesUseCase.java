// 이름으로 장소를 검색하는 유스케이스 계약이다.
package com.sopt.nearby.place.port.in;

import com.sopt.nearby.place.application.SearchPlacesCommand;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;

public interface SearchPlacesUseCase {
    PlaceSearchPage search(SearchPlacesCommand command);
}
