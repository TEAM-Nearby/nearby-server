// 장소 검색 결과와 다음 페이지 및 출처 정보를 표현한다.
package com.sopt.nearby.place.domain.model;

import java.math.BigDecimal;
import java.util.List;

public record PlaceSearchPage(List<Place> places, String nextPageToken) {
    public PlaceSearchPage {
        places = List.copyOf(places);
    }

    public record Place(String googlePlaceId, String name, String address,
                        BigDecimal latitude, BigDecimal longitude, Category category,
                        List<Attribution> attributions) {
        public Place {
            attributions = List.copyOf(attributions);
        }
    }

    public record Attribution(String provider, String providerUri) {
    }

    public enum Category {
        RESTAURANT, CAFE, PUB, MUSEUM, PHOTO_SPOT, OTHER
    }
}
