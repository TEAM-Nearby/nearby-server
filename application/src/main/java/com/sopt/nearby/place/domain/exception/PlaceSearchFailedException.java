// 장소 검색 외부 호출이나 응답의 오류를 표현한다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class PlaceSearchFailedException extends BusinessException {
    public PlaceSearchFailedException() {
        super(PlaceErrorCode.PLACE_SEARCH_FAILED);
    }
}
