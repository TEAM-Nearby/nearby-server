// API 계층에서 발생한 예외를 공통 응답 형식으로 변환하는 전역 핸들러
package com.sopt.nearby.shared.adapter.in.web.exception;

import com.sopt.nearby.common.exception.ErrorCode;
import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.logging.ServerErrorRequestContext;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.common.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(
            final BusinessException exception,
            final HttpServletRequest request
    ) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = ApiExceptionStatusResolver.resolve(exception);
        if (status.is5xxServerError()) {
            ServerErrorRequestContext.record(request, exception);
        }

        return ResponseEntity
                .status(status)
                .body(CommonResponse.error(status.value(), errorCode));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNotFoundException(
            final NotFoundException exception,
            final HttpServletRequest request
    ) {
        return handleBusinessException(exception, request);
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpMessageNotReadableException(
            final HttpMessageNotReadableException exception
    ) {
        return handleGlobalError(GlobalErrorCode.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleHttpRequestMethodNotSupportedException(
            final HttpRequestMethodNotSupportedException exception
    ) {
        return handleGlobalError(GlobalErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(
            final Exception exception,
            final HttpServletRequest request
    ) {
        ServerErrorRequestContext.record(request, exception);

        return handleGlobalError(GlobalErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<CommonResponse<Void>> handleValidationException(final BindException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseGet(() -> exception.getBindingResult()
                        .getGlobalErrors()
                        .stream()
                        .map(ObjectError::getDefaultMessage)
                        .filter(Objects::nonNull)
                        .findFirst()
                        .orElse("올바르지 않은 입력값입니다."));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(CommonResponse.validationError(message));
    }


    private ResponseEntity<CommonResponse<Void>> handleGlobalError(final GlobalErrorCode errorCode) {
        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(CommonResponse.error(errorCode.httpStatus().value(), errorCode));
    }
}
