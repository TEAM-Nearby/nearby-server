// 취소된 동행을 완료 처리하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompleteCompanionMeetingAlreadyCanceledException extends ConflictException {

	public CompleteCompanionMeetingAlreadyCanceledException() {
		super(CompanionErrorCode.COMPLETE_COMPANION_MEETING_ALREADY_CANCELED);
	}
}
