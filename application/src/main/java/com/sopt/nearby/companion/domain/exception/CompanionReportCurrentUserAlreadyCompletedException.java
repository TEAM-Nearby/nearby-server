// 동행을 이미 마친 사용자의 신고를 막는 예외
package com.sopt.nearby.companion.domain.exception;

import com.sopt.nearby.common.exception.BusinessException;
import com.sopt.nearby.companion.domain.code.CompanionErrorCode;

public class CompanionReportCurrentUserAlreadyCompletedException extends BusinessException {

	public CompanionReportCurrentUserAlreadyCompletedException() {
		super(CompanionErrorCode.COMPANION_REPORT_CURRENT_USER_ALREADY_COMPLETED);
	}
}
