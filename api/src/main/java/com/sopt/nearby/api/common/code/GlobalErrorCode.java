// API 전역 예외 상황의 기본 에러 코드를 정의하는 enum
package com.sopt.nearby.api.common.exception;

import com.sopt.nearby.common.code.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GlobalErrorCode implements ErrorCode {
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류가 발생했습니다. 다시 시도해 주세요."),
	;

	private final HttpStatus status;
	private final String message;
}