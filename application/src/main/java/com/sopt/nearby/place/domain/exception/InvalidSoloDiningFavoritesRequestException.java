// 혼밥 맛집 즐겨찾기 목록 조회 요청값이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.place.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class InvalidSoloDiningFavoritesRequestException extends BusinessException {

    public InvalidSoloDiningFavoritesRequestException() {
        super(FavoritesErrorCode.VALIDATION_ERROR);
    }

    private enum FavoritesErrorCode implements ErrorCode {
        VALIDATION_ERROR("위도, 경도, 카테고리, 정렬값 오류가 발생했습니다.");

        private final String message;

        FavoritesErrorCode(final String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
