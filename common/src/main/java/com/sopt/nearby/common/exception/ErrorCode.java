package com.sopt.nearby.common.exception;
// 공통 비즈니스 예외의 에러 코드와 메시지를 표현하는 인터페이스


public interface ErrorCode {

    String name();

    String message();
}
