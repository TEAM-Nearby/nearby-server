// 혼밥 맛집 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.code.PlaceSuccessCode;
import com.sopt.nearby.place.adapter.in.web.dto.request.SoloDiningPlacesRequest;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlacesResponse;
import com.sopt.nearby.place.port.in.ReadSoloDiningPlacesUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solo-dining/places")
public class SoloDiningPlaceController implements SoloDiningPlaceApi {

    private final ReadSoloDiningPlacesUseCase readSoloDiningPlacesUseCase;

    public SoloDiningPlaceController(final ReadSoloDiningPlacesUseCase readSoloDiningPlacesUseCase) {
        this.readSoloDiningPlacesUseCase = readSoloDiningPlacesUseCase;
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
}
