// 공통 비즈니스 예외의 상태 코드와 메시지를 표현하는 인터페이스
package com.sopt.nearby.common.exception;

public interface ErrorCode {

	String name();

	int status();

	String message();
}
