// 장소 기능의 비즈니스 에러 코드를 정의한다.
package com.sopt.nearby.place.domain.code;

import com.sopt.nearby.common.exception.ErrorCode;

public enum PlaceErrorCode implements ErrorCode {

    VALIDATION_ERROR("위도, 경도, 카테고리 요청값 오류가 발생했습니다."),
    PLACE_NOT_FOUND("장소를 찾을 수 없습니다."),
    GOOGLE_PLACE_API_ERROR("Google Places API 호출에 실패했습니다."),
    INVALID_PLACE_SEARCH_REQUEST("검색어, 검색 중심 좌표 또는 검색 조건이 올바르지 않습니다."),
    PLACE_SEARCH_FAILED("장소 검색 서비스 호출에 실패했습니다."),
    PLACE_SEARCH_RATE_LIMITED("장소 검색 서비스의 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요."),
    PLACE_SEARCH_TIMEOUT("장소 검색 응답 시간이 초과되었습니다.");

    private final String message;

    PlaceErrorCode(final String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
