// 동행 후기 대상 사용자를 찾을 수 없을 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.NotFoundException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class RevieweeNotFoundException extends NotFoundException {

	public RevieweeNotFoundException() {
		super(CompanionErrorCode.REVIEWEE_NOT_FOUND);
	}
}
