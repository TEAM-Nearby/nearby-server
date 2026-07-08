// 혼밥 맛집 즐겨찾기 API 문서를 정의한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningFavoritesResponse;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoritesRequestException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;

@Tag(name = "SoloDiningFavorite", description = "혼밥 맛집 즐겨찾기 API")
public interface SoloDiningFavoriteApi {

    @ApiExceptions(InvalidSoloDiningFavoritesRequestException.class)
    @Operation(
            summary = "혼밥 맛집 즐겨찾기 목록 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 즐겨찾기 등록한 혼밥 맛집 목록을 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "즐겨찾기 목록 조회에 성공했습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "status": 200,
                              "code": "SOLO_DINING_FAVORITES_FOUND",
                              "message": "즐겨찾기 목록 조회에 성공했습니다.",
                              "data": {
                                "totalCount": 35,
                                "favorites": [
                                  {
                                    "favoriteId": 5,
                                    "createdAt": "2026-07-02T13:20:00",
                                    "placeId": 12,
                                    "googlePlaceId": "ChIJxxxxxxxx",
                                    "name": "르상크 바르셀로나점",
                                    "photoReference": "places/ChIJxxxxxxxx/photos/ATKogp...",
                                    "category": "CAFE",
                                    "distanceMeters": 800,
                                    "rating": 4.3,
                                    "reviewCount": 22870,
                                    "isFavorite": true,
                                    "businessStatus": "OPERATIONAL"
                                  }
                                ]
                              }
                            }
                            """)
            )
    )
    CommonResponse<SoloDiningFavoritesResponse> getFavorites(
            @Parameter(description = "사용자 현재 위치 위도", required = true, example = "37.56650000")
            String latitude,
            @Parameter(description = "사용자 현재 위치 경도", required = true, example = "126.97800000")
            String longitude,
            @Parameter(description = "음식 카테고리", example = "CAFE")
            String category,
            @Parameter(description = "정렬 기준", example = "LATEST")
            String sort,
            @Parameter(hidden = true)
            Principal principal
    );
}
