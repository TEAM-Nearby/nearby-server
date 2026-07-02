// API 전역 예외 상황의 기본 에러 코드를 정의하는 enum
package com.sopt.nearby.shared.adapter.in.web.exception;


import com.sopt.nearby.common.exception.ErrorCode;

public enum GlobalErrorCode implements ErrorCode {

	BAD_REQUEST(400, "잘못된 요청입니다."),
	METHOD_NOT_ALLOWED(405, "지원하지 않는 HTTP 메서드입니다."),
	INTERNAL_SERVER_ERROR(500, "내부 서버 오류가 발생했습니다. 다시 시도해 주세요.");

	private final int status;
	private final String message;

	GlobalErrorCode(final int status, final String message) {
		this.status = status;
		this.message = message;
	}

	@Override
	public int getStatus() {
		return status;
	}

	@Override
	public String message() {
		return message;
	}
}
