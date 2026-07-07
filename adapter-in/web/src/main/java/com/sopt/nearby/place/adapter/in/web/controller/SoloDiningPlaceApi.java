// 혼밥 맛집 API 문서를 정의한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlacesResponse;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "SoloDiningPlace", description = "혼밥 맛집 API")
public interface SoloDiningPlaceApi {

    @ApiExceptions({
            InvalidSoloDiningPlacesRequestException.class,
            GooglePlaceApiException.class
    })
    @Operation(
            summary = "혼밥 맛집 목록 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 현재 위치 주변의 혼밥 맛집 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    CommonResponse<SoloDiningPlacesResponse> getPlaces(
            @Parameter(description = "사용자 현재 위치 위도", required = true, example = "37.56650000")
            String latitude,
            @Parameter(description = "사용자 현재 위치 경도", required = true, example = "126.97800000")
            String longitude,
            @Parameter(description = "음식 카테고리", example = "CAFE")
            String category,
            @Parameter(hidden = true)
            Principal principal
    );
}
