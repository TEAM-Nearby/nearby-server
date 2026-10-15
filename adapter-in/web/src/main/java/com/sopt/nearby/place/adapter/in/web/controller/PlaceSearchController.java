// 동행글 작성용 장소 검색 HTTP 요청을 처리한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.code.PlaceSuccessCode;
import com.sopt.nearby.place.adapter.in.web.dto.request.PlaceSearchRequest;
import com.sopt.nearby.place.adapter.in.web.dto.response.PlaceSearchResponse;
import com.sopt.nearby.place.port.in.SearchPlacesUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companion-places")
public class PlaceSearchController implements PlaceSearchApi {
    private final SearchPlacesUseCase searchPlaces;

    public PlaceSearchController(final SearchPlacesUseCase searchPlaces) {
        this.searchPlaces = searchPlaces;
    }

    @Override
    @GetMapping("/search")
    public CommonResponse<PlaceSearchResponse> search(
            @RequestParam(required = false) final String query,
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            @RequestParam(required = false) final String radiusMeters,
            @RequestParam(required = false) final String languageCode,
            @RequestParam(required = false) final String pageSize,
            @RequestParam(required = false) final String pageToken
    ) {
        PlaceSearchRequest request = new PlaceSearchRequest(query, latitude, longitude, radiusMeters,
                languageCode, pageSize, pageToken);
        return CommonResponse.success(PlaceSuccessCode.PLACES_SEARCHED,
                PlaceSearchResponse.from(searchPlaces.search(request.toCommand())));
    }
}
