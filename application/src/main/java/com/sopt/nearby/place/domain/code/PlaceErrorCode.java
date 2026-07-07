// 장소 기능의 비즈니스 에러 코드를 정의한다.
package com.sopt.nearby.place.domain.code;

import com.sopt.nearby.common.exception.ErrorCode;

public enum PlaceErrorCode implements ErrorCode {

    VALIDATION_ERROR("위도, 경도, 카테고리 요청값 오류가 발생했습니다."),
    GOOGLE_PLACE_API_ERROR("Google Places API 호출에 실패했습니다.");

    private final String message;

    PlaceErrorCode(final String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
