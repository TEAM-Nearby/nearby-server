// 이미 같은 대상에게 동행 후기를 남겼을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReviewAlreadyExistsException extends ConflictException {

	public CompanionReviewAlreadyExistsException() {
		super(CompanionErrorCode.COMPANION_REVIEW_ALREADY_EXISTS);
	}
}
