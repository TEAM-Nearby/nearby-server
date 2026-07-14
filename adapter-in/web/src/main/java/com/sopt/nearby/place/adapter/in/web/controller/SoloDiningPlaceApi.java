// 혼밥 맛집 API 문서를 정의한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlaceResponse;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningFavoriteResponse;
import com.sopt.nearby.place.adapter.in.web.dto.response.SoloDiningPlacesResponse;
import com.sopt.nearby.place.domain.exception.GooglePlaceApiException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningFavoriteRequestException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlaceRequestException;
import com.sopt.nearby.place.domain.exception.InvalidSoloDiningPlacesRequestException;
import com.sopt.nearby.place.domain.exception.PlaceNotFoundException;
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
    @ApiResponse(
            responseCode = "200",
            description = "혼밥 맛집 목록 조회에 성공했습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "status": 200,
                              "code": "SOLO_DINING_PLACES_FOUND",
                              "message": "혼밥 맛집 목록 조회에 성공했습니다.",
                              "data": {
                                "places": [
                                  {
                                    "placeId": 12,
                                    "googlePlaceId": "ChIJxxxxxxxx",
                                    "name": "르상크 바르셀로나점",
                                    "address": "서울특별시 중구 세종대로 110",
                                    "photoReference": "places/ChIJxxxxxxxx/photos/ATKogp...",
                                    "imageUrl": "https://lh3.googleusercontent.com/place.jpg",
                                    "category": "CAFE",
                                    "distanceMeters": 800,
                                    "rating": 4.3,
                                    "reviewCount": 22870,
                                    "isFavorite": false,
                                    "latitude": 37.56612000,
                                    "longitude": 126.97845000,
                                    "businessStatus": "OPERATIONAL"
                                  }
                                ]
                              }
                            }
                            """)
            )
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

    @ApiExceptions({
            InvalidSoloDiningPlaceRequestException.class,
            PlaceNotFoundException.class,
            GooglePlaceApiException.class
    })
    @Operation(
            summary = "혼밥 맛집 상세 조회",
            description = "JWT 액세스 토큰으로 인증된 사용자가 혼밥 맛집 상세 정보를 조회합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "혼밥 맛집 상세 조회에 성공했습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "status": 200,
                              "code": "SOLO_DINING_PLACE_FOUND",
                              "message": "혼밥 맛집 상세 조회에 성공했습니다.",
                              "data": {
                                "placeId": 12,
                                "googlePlaceId": "ChIJxxxxxxxx",
                                "name": "르상크 바르셀로나점",
                                "address": "서울특별시 중구 세종대로 110",
                                "latitude": 37.56612000,
                                "longitude": 126.97845000,
                                "category": "CAFE",
                                "distanceMeters": 800,
                                "rating": 4.3,
                                "reviewCount": 22870,
                                "phoneNumber": "02-1234-5678",
                                "photoReference": "places/ChIJxxxxxxxx/photos/ATKogp...",
                                "photoReferences": ["places/ChIJxxxxxxxx/photos/ATKogp..."],
                                "imageUrl": "https://lh3.googleusercontent.com/place.jpg",
                                "businessStatus": "OPERATIONAL",
                                "priceLevel": "PRICE_LEVEL_MODERATE",
                                "priceRange": "₩10,000~₩20,000",
                                "regularOpeningHours": ["월요일: 오전 11:00~오후 9:00"],
                                "editorialSummary": "혼밥하기 좋은 조용한 식당입니다.",
                                "isFavorite": true
                              }
                            }
                            """)
            )
    )
    CommonResponse<SoloDiningPlaceResponse> getPlace(
            @Parameter(description = "Nearby 내부 장소 ID", required = true, example = "12")
            String placeId,
            @Parameter(description = "사용자 현재 위치 위도", required = true, example = "37.56650000")
            String latitude,
            @Parameter(description = "사용자 현재 위치 경도", required = true, example = "126.97800000")
            String longitude,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidSoloDiningFavoriteRequestException.class,
            PlaceNotFoundException.class
    })
    @Operation(
            summary = "혼밥 맛집 즐겨찾기 등록",
            description = "JWT 액세스 토큰으로 인증된 사용자가 혼밥 맛집을 즐겨찾기에 등록합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "혼밥 맛집 즐겨찾기 등록에 성공했습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "status": 200,
                              "code": "SOLO_DINING_FAVORITE_REGISTERED",
                              "message": "식당 즐겨찾기 등록에 성공했습니다.",
                              "data": {
                                "isFavorite": true
                              }
                            }
                            """)
            )
    )
    CommonResponse<SoloDiningFavoriteResponse> registerFavorite(
            @Parameter(description = "Nearby 내부 장소 ID", required = true, example = "12")
            String placeId,
            @Parameter(hidden = true)
            Principal principal
    );

    @ApiExceptions({
            InvalidSoloDiningFavoriteRequestException.class,
            PlaceNotFoundException.class
    })
    @Operation(
            summary = "혼밥 맛집 즐겨찾기 해제",
            description = "JWT 액세스 토큰으로 인증된 사용자가 혼밥 맛집 즐겨찾기를 해제합니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponse(
            responseCode = "200",
            description = "혼밥 맛집 즐겨찾기 해제에 성공했습니다.",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {
                              "status": 200,
                              "code": "SOLO_DINING_FAVORITE_REMOVED",
                              "message": "식당 즐겨찾기 해제에 성공했습니다.",
                              "data": {
                                "isFavorite": false
                              }
                            }
                            """)
            )
    )
    CommonResponse<SoloDiningFavoriteResponse> removeFavorite(
            @Parameter(description = "Nearby 내부 장소 ID", required = true, example = "12")
            String placeId,
            @Parameter(hidden = true)
            Principal principal
    );
}
