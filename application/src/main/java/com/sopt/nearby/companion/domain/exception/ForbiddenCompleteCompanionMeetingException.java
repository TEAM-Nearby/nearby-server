// 동행 참여자가 아닌 사용자가 동행을 마치려 할 때 발생하는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class ForbiddenCompleteCompanionMeetingException extends BusinessException {

	public ForbiddenCompleteCompanionMeetingException() {
		super(CompanionErrorCode.FORBIDDEN_COMPLETE_COMPANION_MEETING);
	}
}
