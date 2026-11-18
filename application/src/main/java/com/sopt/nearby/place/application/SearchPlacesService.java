// 외부 검색 결과에서 동행글 작성에 사용할 수 있는 장소만 반환한다.
package com.sopt.nearby.place.application;

import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import com.sopt.nearby.place.domain.model.PlaceSearchPage;
import com.sopt.nearby.place.domain.model.PlaceSearchPage.Category;
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
        PlaceSearchPage page = searchPort.search(command);
        return new PlaceSearchPage(page.places().stream()
                .filter(place -> place.category() != null && place.category() != Category.OTHER)
                .toList(), page.nextPageToken());
    }
}
