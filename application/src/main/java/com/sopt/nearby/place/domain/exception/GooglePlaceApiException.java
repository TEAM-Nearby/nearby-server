// Google Places API 호출 실패를 표현하는 예외다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class GooglePlaceApiException extends BusinessException {

    public GooglePlaceApiException() {
        super(PlaceErrorCode.GOOGLE_PLACE_API_ERROR);
    }
}
