// 후기 대상 사용자가 만남 인증을 하지 않았을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class RevieweeNotCheckedInException extends ConflictException {

	public RevieweeNotCheckedInException() {
		super(CompanionErrorCode.REVIEWEE_NOT_CHECKED_IN);
	}
}
