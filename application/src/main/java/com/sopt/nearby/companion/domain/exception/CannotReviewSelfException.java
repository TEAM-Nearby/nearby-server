// 자기 자신에게 동행 후기를 남기려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CannotReviewSelfException extends BusinessException {

	public CannotReviewSelfException() {
		super(CompanionErrorCode.CANNOT_REVIEW_SELF);
	}
}
