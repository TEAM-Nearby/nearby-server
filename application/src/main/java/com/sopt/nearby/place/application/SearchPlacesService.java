// 검증된 장소 검색을 외부 검색 포트로 전달한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;
import com.sopt.nearby.place.port.in.SearchPlacesUseCase;
import com.sopt.nearby.place.port.out.PlaceTextSearchPort;

public class SearchPlacesService implements SearchPlacesUseCase {
    private final PlaceTextSearchPort searchPort;

    public SearchPlacesService(final PlaceTextSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    @Override
    public PlaceSearchPage search(final SearchPlacesCommand command) {
        if (command == null) {
            throw new InvalidPlaceSearchRequestException();
        }
        return searchPort.search(command);
    }
}
