// 완료된 동행의 후기 대상 목록을 GUEST가 조회하려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.ConflictException;
import com.sopt.nearby.common.exception.ErrorCode;

public class CompanionReviewTargetMeetingAlreadyCompletedException extends ConflictException {

	public CompanionReviewTargetMeetingAlreadyCompletedException() {
		super(ReviewTargetMeetingErrorCode.COMPANION_MEETING_ALREADY_COMPLETED);
	}

	private enum ReviewTargetMeetingErrorCode implements ErrorCode {
		COMPANION_MEETING_ALREADY_COMPLETED("이미 완료된 동행입니다.");

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
