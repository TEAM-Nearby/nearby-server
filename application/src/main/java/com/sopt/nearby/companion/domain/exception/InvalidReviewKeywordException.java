// 올바르지 않은 동행 후기 키워드일 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class InvalidReviewKeywordException extends BusinessException {

	public InvalidReviewKeywordException() {
		super(CompanionErrorCode.INVALID_REVIEW_KEYWORD);
	}
}
