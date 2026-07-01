// API 성공과 실패 응답을 동일한 구조로 감싸는 응답 래퍼
package com.sopt.nearby.shared.adapter.in.web.response;

import com.sopt.nearby.common.exception.ErrorCode;

public record CommonResponse<T>(
	int status,
	String code,
	String message,
	T data
) {

	private static final int SUCCESS_STATUS = 200;
	private static final int VALIDATION_ERROR_STATUS = 400;
	private static final String VALIDATION_ERROR_CODE = "VALIDATION_ERROR";

	public static <T> CommonResponse<T> success(final SuccessCode successCode, final T data) {
		return new CommonResponse<>(SUCCESS_STATUS, successCode.name(), successCode.message(), data);
	}

	public static CommonResponse<Void> success(final SuccessCode successCode) {
		return success(successCode, null);
	}

	public static CommonResponse<Void> error(final ErrorCode errorCode) {
		return new CommonResponse<>(errorCode.status(), errorCode.name(), errorCode.message(), null);
	}

	public static CommonResponse<Void> validationError(final String validationErrorMessage) {
		return new CommonResponse<>(VALIDATION_ERROR_STATUS, VALIDATION_ERROR_CODE, validationErrorMessage, null);
	}
}
