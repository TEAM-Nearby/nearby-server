// 혼밥 맛집 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.code.PlaceSuccessCode;
import com.sopt.nearby.place.adapter.in.web.dto.request.SoloDiningPlaceRequest;
import com.sopt.nearby.place.adapter.in.web.dto.request.SoloDiningPlacesRequest;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlaceResponse;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlacesResponse;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlaceUseCase;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solo-dining/places")
public class SoloDiningPlaceController implements SoloDiningPlaceApi {

    private final ReadSoloDiningPlacesUseCase readSoloDiningPlacesUseCase;
    private final ReadSoloDiningPlaceUseCase readSoloDiningPlaceUseCase;

    public SoloDiningPlaceController(
            final ReadSoloDiningPlacesUseCase readSoloDiningPlacesUseCase,
            final ReadSoloDiningPlaceUseCase readSoloDiningPlaceUseCase
    ) {
        this.readSoloDiningPlacesUseCase = readSoloDiningPlacesUseCase;
        this.readSoloDiningPlaceUseCase = readSoloDiningPlaceUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<SoloDiningPlacesResponse> getPlaces(
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            @RequestParam(required = false) final String category,
            final Principal principal
    ) {
        SoloDiningPlacesRequest request = new SoloDiningPlacesRequest(latitude, longitude, category);

        return CommonResponse.success(
                PlaceSuccessCode.SOLO_DINING_PLACES_FOUND,
                SoloDiningPlacesResponse.from(readSoloDiningPlacesUseCase.read(
                        request.toCommand(Long.valueOf(principal.getName()))
                ))
        );
    }

    @Override
    @GetMapping("/{placeId}")
    public CommonResponse<SoloDiningPlaceResponse> getPlace(
            @PathVariable final String placeId,
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            final Principal principal
    ) {
        SoloDiningPlaceRequest request = new SoloDiningPlaceRequest(placeId, latitude, longitude);

        return CommonResponse.success(
                PlaceSuccessCode.SOLO_DINING_PLACE_FOUND,
                SoloDiningPlaceResponse.from(readSoloDiningPlaceUseCase.read(
                        request.toCommand(Long.valueOf(principal.getName()))
                ))
        );
    }
}
