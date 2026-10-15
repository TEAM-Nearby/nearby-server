// 혼밥 맛집 목록 조회 요청값이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.place.domain.code.PlaceErrorCode;

public class InvalidSoloDiningPlacesRequestException extends BusinessException {

    public InvalidSoloDiningPlacesRequestException() {
        super(PlaceErrorCode.VALIDATION_ERROR);
    }
}
