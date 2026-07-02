// API 계층에서 발생한 예외를 공통 응답 형식으로 변환하는 전역 핸들러
package com.sopt.nearby.shared.adapter.in.web.exception;

import com.sopt.nearby.common.exception.ErrorCode;
import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.shared.adapter.in.web.response.CommonResponse;
import com.sopt.nearby.common.exception.BusinessException;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(final BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        HttpStatus status = resolveBusinessErrorStatus(errorCode);

        return ResponseEntity
                .status(status)
                .body(CommonResponse.error(status.value(), errorCode));
    }

    private HttpStatus resolveBusinessErrorStatus(final ErrorCode errorCode) {
        return switch (errorCode.name()) {
            //Todo 추후 feature별 ErrorCode가 추가되면 이곳에서 HTTP status로 매핑합니다.
//            case "USER_NOT_FOUND",
//                 "PLACE_NOT_FOUND",
//                 "COMPANION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
//
//            case "ALREADY_EXISTS",
//                 "ALREADY_APPLIED",
//                 "DUPLICATED_USER" -> HttpStatus.CONFLICT;
//
//            case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
//
//            case "FORBIDDEN" -> HttpStatus.FORBIDDEN;

            default -> HttpStatus.BAD_REQUEST;
        };
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<CommonResponse<Void>> handleNotFoundException(final NotFoundException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(CommonResponse.error(HttpStatus.NOT_FOUND.value(), errorCode));
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
    public ResponseEntity<CommonResponse<Void>> handleException(final Exception exception) {
        log.error("처리되지 않은 예외가 발생했습니다.", exception);

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
