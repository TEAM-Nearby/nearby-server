// API 성공과 실패 응답을 동일한 구조로 감싸는 응답 래퍼
package com.sopt.nearby.api.common.response;

import com.sopt.nearby.common.exception.ErrorCode;

public record CommonResponse<T>(
	int status,
	String code,
	String message,
	T data
) {

	public static <T> CommonResponse<T> success(final SuccessCode successCode, final T data) {
		return new CommonResponse<>(200, successCode.name(), successCode.message(), data);
	}

	public static CommonResponse<Void> success(final SuccessCode successCode) {
		return success(successCode, null);
	}

	public static CommonResponse<Void> error(final ErrorCode errorCode) {
		return new CommonResponse<>(errorCode.status(), errorCode.name(), errorCode.message(), null);
	}

	public static CommonResponse<Void> validationError(final String validationErrorMessage) {
		return new CommonResponse<>(400, "VALIDATION_ERROR", validationErrorMessage, null);
	}
}
