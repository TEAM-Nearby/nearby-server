// 리소스를 찾지 못한 비즈니스 예외를 표현하는 공통 예외
package com.sopt.nearby.common.exception;


public class NotFoundException extends BusinessException {

    public NotFoundException(final ErrorCode errorCode) {
        super(errorCode);
    }
}