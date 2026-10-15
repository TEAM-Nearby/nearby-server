// 장소를 찾지 못했을 때 발생하는 예외다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class PlaceNotFoundException extends NotFoundException {

    public PlaceNotFoundException() {
        super(PlaceErrorCode.PLACE_NOT_FOUND);
    }
}
