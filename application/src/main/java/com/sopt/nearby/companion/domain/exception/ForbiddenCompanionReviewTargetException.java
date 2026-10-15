// 동행 참여자가 아닌 사용자가 후기 대상 목록을 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompanionReviewTargetException extends BusinessException {

	public ForbiddenCompanionReviewTargetException() {
		super(CompanionErrorCode.FORBIDDEN_COMPANION_REVIEW_TARGET);
	}
}
