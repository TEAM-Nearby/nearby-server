// 동행 후기 키워드 선택 개수가 올바르지 않을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidReviewKeywordCountException extends BusinessException {

	public InvalidReviewKeywordCountException() {
		super(CompanionErrorCode.INVALID_REVIEW_KEYWORD_COUNT);
	}
}
