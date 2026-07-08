// 현재 사용자가 만남 인증을 하지 않은 상태에서 후기를 등록할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CurrentUserNotCheckedInException extends ConflictException {

	public CurrentUserNotCheckedInException() {
		super(CompanionErrorCode.CURRENT_USER_NOT_CHECKED_IN);
	}
}
