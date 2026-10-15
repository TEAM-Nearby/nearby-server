// 취소된 동행에 후기를 남기려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReviewMeetingAlreadyCanceledException extends ConflictException {

	public CompanionReviewMeetingAlreadyCanceledException() {
		super(CompanionErrorCode.COMPANION_REVIEW_MEETING_ALREADY_CANCELED);
	}
}
