// 외부 장소 검색의 응답 시간 초과를 표현한다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class PlaceSearchTimeoutException extends BusinessException {
    public PlaceSearchTimeoutException() {
        super(PlaceErrorCode.PLACE_SEARCH_TIMEOUT);
    }
}
