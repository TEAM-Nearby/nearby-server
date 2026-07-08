// 혼밥 맛집 상세 조회 요청값이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class InvalidSoloDiningPlaceRequestException extends BusinessException {

    public InvalidSoloDiningPlaceRequestException() {
        super(DetailErrorCode.VALIDATION_ERROR);
    }

    private enum DetailErrorCode implements ErrorCode {
        VALIDATION_ERROR("placeId, 위도, 경도 요청값 오류가 발생했습니다.");

        private final String message;

        DetailErrorCode(final String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
