// 외부 장소 검색의 호출 제한을 표현한다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class PlaceSearchRateLimitedException extends BusinessException {
    public PlaceSearchRateLimitedException() {
        super(PlaceErrorCode.PLACE_SEARCH_RATE_LIMITED);
    }
}
