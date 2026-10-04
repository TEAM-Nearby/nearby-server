// 잘못된 장소 검색 조건을 표현한다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class InvalidPlaceSearchRequestException extends BusinessException {
    public InvalidPlaceSearchRequestException() {
        super(PlaceErrorCode.INVALID_PLACE_SEARCH_REQUEST);
    }
}
