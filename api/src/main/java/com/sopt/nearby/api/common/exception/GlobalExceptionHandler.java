// API 계층에서 발생한 예외를 공통 응답 형식으로 변환하는 전역 핸들러
package com.sopt.nearby.api.common.exception;

import com.sopt.nearby.api.common.response.CommonResponse;
import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<CommonResponse<Void>> handleBusinessException(final BusinessException exception) {
		ErrorCode errorCode = exception.getErrorCode();

		return ResponseEntity
			.status(HttpStatusCode.valueOf(errorCode.status()))
			.body(CommonResponse.error(errorCode));
	}

	@ExceptionHandler(BindException.class)
	public ResponseEntity<CommonResponse<Void>> handleBindException(final BindException exception) {
		return handleValidationException(exception);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<CommonResponse<Void>> handleMethodArgumentNotValidException(
		final MethodArgumentNotValidException exception
	) {
		return handleValidationException(exception);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<CommonResponse<Void>> handleException(final Exception exception) {
		log.error("처리되지 않은 예외가 발생했습니다.", exception);

		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(CommonResponse.error(GlobalErrorCode.INTERNAL_SERVER_ERROR));
	}

	private ResponseEntity<CommonResponse<Void>> handleValidationException(final BindException exception) {
		String message = exception.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(FieldError::getDefaultMessage)
			.findFirst()
			.orElse("올바르지 않은 입력값입니다.");

		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(CommonResponse.validationError(message));
	}
}
