// 장소 검색 결과를 동행글 작성에 사용할 HTTP 응답으로 변환한다.
package com.sopt.nearby.place.adapter.in.web.dto.response;

import com.sopt.nearby.place.domain.model.PlaceSearchPage;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record PlaceSearchResponse(List<Place> places,
                                  @Schema(description = "다음 페이지 토큰. 없으면 null이며 후속 요청은 같은 검색 조건을 사용합니다.")
                                  String nextPageToken) {
    public static PlaceSearchResponse from(final PlaceSearchPage page) {
        return new PlaceSearchResponse(page.places().stream().map(place -> new Place(
                place.googlePlaceId(), place.name(), place.address(), place.latitude(), place.longitude(),
                place.category().name(), place.attributions().stream()
                        .map(value -> new Attribution(value.provider(), value.providerUri())).toList()
        )).toList(), page.nextPageToken());
    }

    public record Place(String googlePlaceId, String name,
                        @Schema(description = "Google이 주소를 제공하지 않으면 null입니다.") String address,
                        BigDecimal latitude, BigDecimal longitude,
                        @Schema(allowableValues = {"RESTAURANT", "CAFE", "PUB", "MUSEUM", "PHOTO_SPOT", "OTHER"})
                        String category, List<Attribution> attributions) {
    }

    public record Attribution(String provider, String providerUri) {
    }
}
