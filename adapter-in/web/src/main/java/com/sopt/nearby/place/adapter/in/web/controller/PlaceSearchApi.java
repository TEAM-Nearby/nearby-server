// 동행글 작성용 장소 검색의 입력·출력과 오류 계약을 문서화한다.
package com.sopt.nearby.place.adapter.in.web.controller;

import com.sopt.nearby.place.adapter.in.web.dto.response.PlaceSearchResponse;
import com.sopt.nearby.place.domain.exception.InvalidPlaceSearchRequestException;
import com.sopt.nearby.place.domain.exception.PlaceSearchFailedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchRateLimitedException;
import com.sopt.nearby.place.domain.exception.PlaceSearchTimeoutException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.shared.adapter.in.web.swagger.ApiExceptions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "동행 장소 검색", description = "동행글 작성 시 선택할 장소를 검색합니다.")
public interface PlaceSearchApi {
    @Operation(summary = "동행글 작성용 장소 검색",
            description = "검색어와 선택 도시 또는 지도 중심 좌표로 Google Text Search를 호출합니다. "
                    + "좌표는 필수이며 radiusMeters는 검색 우선순위일 뿐 결과의 경계가 아닙니다. "
                    + "기본 표시 언어는 한국어이며 모든 한국어 별칭의 검색을 보장하지 않습니다. "
                    + "최대 20건을 반환하며 빈 결과도 200입니다. 다음 페이지는 같은 검색 조건과 nextPageToken을 사용합니다. "
                    + "온보딩 완료와 활성 동행 프로필이 필요합니다. 선택한 결과의 googlePlaceId, name, address, "
                    + "latitude, longitude, category를 기존 동행글 작성 요청의 place에 전달합니다. "
                    + "앱은 Google Maps 출처 표시 및 반환된 attributions를 표시해야 합니다.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiExceptions({InvalidPlaceSearchRequestException.class, PlaceSearchFailedException.class,
            PlaceSearchRateLimitedException.class, PlaceSearchTimeoutException.class})
    @ApiResponse(responseCode = "200", description = "장소 검색 성공. 검색 결과가 없으면 places가 빈 배열입니다.")
    @ApiResponse(responseCode = "401", description = "인증이 필요합니다.")
    @ApiResponse(responseCode = "403", description = "온보딩 또는 동행 프로필 등록이 필요합니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples = {
                    @ExampleObject(name = "ONBOARDING_REQUIRED", value = """
                            {"status":403,"code":"ONBOARDING_REQUIRED","message":"온보딩 과정이 완료되지 않았습니다.","data":null}
                            """),
                    @ExampleObject(name = "COMPANION_PROFILE_REQUIRED", value = """
                            {"status":403,"code":"COMPANION_PROFILE_REQUIRED","message":"동행 프로필 등록이 필요합니다.","data":null}
                            """)
            }))
    @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없습니다.",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = CommonResponse.class), examples =
                    @ExampleObject(name = "USER_NOT_FOUND", value = """
                            {"status":404,"code":"USER_NOT_FOUND","message":"사용자를 찾을 수 없습니다.","data":null}
                            """)))
    CommonResponse<PlaceSearchResponse> search(
            @Parameter(required = true, description = "앞뒤 공백 정리 후 1~200자 검색어", example = "시우다드 콘달") String query,
            @Parameter(required = true, description = "선택 도시·지도 중심의 위도 (-90~90)", example = "41.3874") String latitude,
            @Parameter(required = true, description = "선택 도시·지도 중심의 경도 (-180~180)", example = "2.1686") String longitude,
            @Parameter(description = "위치 우선순위 반경. 1~50000m, 기본 20000m") String radiusMeters,
            @Parameter(description = "표시 언어. 기본 ko", schema = @Schema(allowableValues = {"ko", "en", "es", "fr", "ja", "de", "it", "pt"})) String languageCode,
            @Parameter(description = "페이지 크기. 1~20, 기본 20") String pageSize,
            @Parameter(description = "이전 응답의 nextPageToken. 검색 조건 변경 시 폐기하며 최대 4096자") String pageToken
    );
}
