// 리소스 상태 충돌로 요청을 처리할 수 없을 때 사용하는 공통 예외
package com.sopt.nearby.common.exception;

public class ConflictException extends BusinessException {

    public ConflictException(final ErrorCode errorCode) {
        super(errorCode);
    }
}