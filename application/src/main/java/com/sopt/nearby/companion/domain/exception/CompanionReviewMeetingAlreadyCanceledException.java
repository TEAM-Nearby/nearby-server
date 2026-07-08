// 취소된 동행에 후기를 남기려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;

public class CompanionReviewMeetingAlreadyCanceledException extends ConflictException {

	public CompanionReviewMeetingAlreadyCanceledException() {
		super(CompanionReviewErrorCode.COMPANION_MEETING_ALREADY_CANCELED);
	}

	private enum CompanionReviewErrorCode implements ErrorCode {
		COMPANION_MEETING_ALREADY_CANCELED("취소된 동행에는 후기를 남길 수 없습니다.");

		private final String message;

		CompanionReviewErrorCode(final String message) {
			this.message = message;
		}

		@Override
		public String message() {
			return message;
		}
	}
}
