// 장소 API 성공 코드를 정의한다.
package com.sopt.nearby.place.adapter.in.web.code;

import com.sopt.nearby.shared.adapter.in.web.response.SuccessCode;

public enum PlaceSuccessCode implements SuccessCode {

    SOLO_DINING_PLACES_FOUND("혼밥 맛집 목록 조회에 성공했습니다."),
    SOLO_DINING_PLACE_FOUND("혼밥 맛집 상세 조회에 성공했습니다."),
    SOLO_DINING_FAVORITE_REGISTERED("식당 즐겨찾기 등록에 성공했습니다."),
    SOLO_DINING_FAVORITE_REMOVED("식당 즐겨찾기 해제에 성공했습니다.");

    private final String message;

    PlaceSuccessCode(final String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
