// 만남 인증을 완료하지 않은 사용자가 동행을 마치려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompleteCompanionMeetingCurrentUserNotCheckedInException extends ConflictException {

	public CompleteCompanionMeetingCurrentUserNotCheckedInException() {
		super(CompanionErrorCode.COMPLETE_COMPANION_MEETING_CURRENT_USER_NOT_CHECKED_IN);
	}
}
