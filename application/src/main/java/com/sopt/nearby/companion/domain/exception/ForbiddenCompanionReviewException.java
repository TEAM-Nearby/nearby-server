// 동행 참여자가 아닌 사용자가 후기를 등록하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionReviewException extends BusinessException {

	public ForbiddenCompanionReviewException() {
		super(CompanionErrorCode.FORBIDDEN_COMPANION_REVIEW);
	}
}
