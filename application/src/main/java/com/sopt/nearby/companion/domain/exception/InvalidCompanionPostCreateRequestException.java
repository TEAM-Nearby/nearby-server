// 동행 모집 글 작성 요청 값이 올바르지 않을 때 발생하는 예외다.
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;

public class InvalidCompanionPostCreateRequestException extends BusinessException {

    public InvalidCompanionPostCreateRequestException() {
        super(CreatePostErrorCode.VALIDATION_ERROR);
    }

    private enum CreatePostErrorCode implements ErrorCode {
        VALIDATION_ERROR("필수값 누락, 인원 범위 오류, 소개 글자 수 초과, 장소 좌표 오류, 만남 시간 입력 규칙 위반입니다.");

        private final String message;

        CreatePostErrorCode(final String message) {
            this.message = message;
        }

        @Override
        public String message() {
            return message;
        }
    }
}
