// 이미 완료된 동행을 다시 완료 처리하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompleteCompanionMeetingAlreadyCompletedException extends ConflictException {

	public CompleteCompanionMeetingAlreadyCompletedException() {
		super(CompanionErrorCode.COMPLETE_COMPANION_MEETING_ALREADY_COMPLETED);
	}
}
