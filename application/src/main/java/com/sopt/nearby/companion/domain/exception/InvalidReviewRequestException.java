// 올바르지 않은 동행 후기 등록 요청일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidReviewRequestException extends BusinessException {

	public InvalidReviewRequestException() {
		super(CompanionErrorCode.INVALID_REVIEW_REQUEST);
	}
}
