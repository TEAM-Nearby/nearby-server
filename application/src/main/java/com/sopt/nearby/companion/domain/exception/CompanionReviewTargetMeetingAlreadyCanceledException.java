// 취소된 동행의 후기 대상 목록을 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;

public class CompanionReviewTargetMeetingAlreadyCanceledException extends ConflictException {

	public CompanionReviewTargetMeetingAlreadyCanceledException() {
		super(ReviewTargetMeetingErrorCode.COMPANION_MEETING_ALREADY_CANCELED);
	}

	private enum ReviewTargetMeetingErrorCode implements ErrorCode {
		COMPANION_MEETING_ALREADY_CANCELED("취소된 동행에는 후기를 남길 수 없습니다.");

		private final String message;

		ReviewTargetMeetingErrorCode(final String message) {
			this.message = message;
		}

		@Override
		public String message() {
			return message;
		}
	}
}
