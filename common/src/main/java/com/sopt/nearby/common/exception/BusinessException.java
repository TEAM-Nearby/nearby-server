// 도메인과 애플리케이션에서 공통으로 사용하는 비즈니스 예외
package com.sopt.nearby.common.exception;

public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(final ErrorCode errorCode) {
		super(errorCode.message());
		this.errorCode = errorCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}
}
