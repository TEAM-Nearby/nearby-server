// 혼밥 맛집 즐겨찾기 HTTP 요청을 유스케이스로 전달한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.code.PlaceSuccessCode;
import com.sopt.nearby.place.adapter.in.web.dto.request.SoloDiningFavoritesRequest;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningFavoritesResponse;
import com.sopt.nearby.place.port.in.ReadSoloDiningFavoritesUseCase;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/solo-dining/favorites")
public class SoloDiningFavoriteController implements SoloDiningFavoriteApi {

    private final ReadSoloDiningFavoritesUseCase readSoloDiningFavoritesUseCase;

    public SoloDiningFavoriteController(final ReadSoloDiningFavoritesUseCase readSoloDiningFavoritesUseCase) {
        this.readSoloDiningFavoritesUseCase = readSoloDiningFavoritesUseCase;
    }

    @Override
    @GetMapping
    public CommonResponse<SoloDiningFavoritesResponse> getFavorites(
            @RequestParam(required = false) final String latitude,
            @RequestParam(required = false) final String longitude,
            @RequestParam(required = false) final String category,
            @RequestParam(required = false) final String sort,
            final Principal principal
    ) {
        SoloDiningFavoritesRequest request = new SoloDiningFavoritesRequest(latitude, longitude, category, sort);

        return CommonResponse.success(
                PlaceSuccessCode.SOLO_DINING_FAVORITES_FOUND,
                SoloDiningFavoritesResponse.from(readSoloDiningFavoritesUseCase.read(
                        request.toCommand(resolveUserId(principal))
                ))
        );
    }

    private Long resolveUserId(final Principal principal) {
        return Long.valueOf(principal.getName());
    }
}
